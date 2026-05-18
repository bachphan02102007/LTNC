package network;

import exception.AuctionClosedException;
import exception.InvalidBidException;
import model.*;
import util.Factory.ItemFactory;
import util.Singleton.AuctionManager;
import util.DataStorage;
import util.Singleton.UserManager;

import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Collectors;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String clientName;
    private String clientRoleHint;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String hello = in.readLine();
            if (hello != null && hello.contains("|")) {
                String[] helloParts = hello.split("\\|", 2);
                clientName = helloParts[0];
                clientRoleHint = helloParts.length > 1 ? helloParts[1] : null;
            } else {
                clientName = hello;
                clientRoleHint = null;
            }
            System.out.println(clientName + " da tham gia!"
                    + (clientRoleHint == null ? "" : " role=" + clientRoleHint));
            sendWalletInfo();

            String onlineList = AuctionServer.getConnectedClients().stream()
                    .filter(c -> c != this)
                    .map(ClientHandler::getClientName)
                    .filter(name -> name != null)
                    .collect(Collectors.joining(", "));

            out.println(onlineList.isEmpty()
                    ? "ONLINE:Ban la nguoi dau tien trong phong!"
                    : "ONLINE:" + onlineList);

            AuctionServer.broadcast(clientName + " da tham gia phong!", this);

            String message;
            while ((message = in.readLine()) != null) {
                System.out.println("[" + clientName + "]: " + message);
                handleCommand(message);
            }
        } catch (IOException e) {
            System.out.println(clientName + " mat ket noi: " + e.getMessage());
        } finally {
            AuctionServer.removeClient(this);
            closeConnection();
        }
    }

    private void handleCommand(String message) {
        try {
            if (message.startsWith("BID:")) {
                handleBid(message);
            } else if (message.equals("LIST")) {
                sendAuctionList();
            } else if (message.startsWith("ADD_ITEM:")) {
                handleAddItem(message);
            } else if (message.startsWith("WALLET:")) {
                handleWallet(message);
            } else if (message.startsWith("PROFILE:UPDATE:")) {
                handleProfileUpdate(message);
            } else if (message.startsWith("PAY:")) {
                handlePay(message);
            } else if (message.startsWith("DETAIL:")) {
                handleDetail(message);
            } else if (message.equals("MY_AUCTIONS")) {
                handleMyAuctions();
            } else if (message.equals("MY_BIDDER_AUCTIONS")) {
                handleMyBidderAuctions();
            } else if (message.startsWith("DELETE_AUCTION:")) {
                handleDeleteAuction(message);
            } else if (message.startsWith("CANCEL_AUCTION:")) {
                handleCancelAuction(message);
            } else if (message.startsWith("LOCK_USER:")) {
                handleUserStatus(message, false);
            } else if (message.startsWith("UNLOCK_USER:")) {
                handleUserStatus(message, true);
            } else {
                out.println("UNKNOWN_COMMAND");
            }
        } catch (Exception e) {
            out.println("ERROR:" + e.getMessage());
        }
    }

    private Optional<User> currentUser() {
        // Server và Client là 2 JVM khác nhau. Mỗi thao tác quan trọng đồng bộ lại users.dat
        // rồi trả về chính object trong UserManager để mọi thay đổi ví/hồ sơ được lưu đúng.
        java.util.List<User> latestUsers = DataStorage.loadUsers();
        if (!latestUsers.isEmpty()) {
            UserManager.getInstance().loadFrom(latestUsers);
        }

        Optional<User> found = UserManager.getInstance().findByUsername(clientName);
        if (found.isPresent()) return found;

        // Fallback chỉ dùng để demo khi lệch thư mục data, không cấp ví cho Seller/Admin.
        if (clientRoleHint != null && clientRoleHint.equalsIgnoreCase("SELLER")) {
            Seller temp = new Seller("TEMP-" + clientName, clientName, "", "");
            return Optional.of(temp);
        }
        if (clientRoleHint != null && clientRoleHint.equalsIgnoreCase("ADMIN")) {
            Admin temp = new Admin("TEMP-" + clientName, clientName, "", "");
            return Optional.of(temp);
        }
        if (clientRoleHint != null && clientRoleHint.equalsIgnoreCase("BIDDER")) {
            Bidder temp = new Bidder("TEMP-" + clientName, clientName, "", "");
            return Optional.of(temp);
        }
        return Optional.empty();
    }

    private void handleBid(String message) {
        String[] parts = message.split(":");
        if (parts.length < 3) {
            out.println("BID_FAIL:Lenh dat gia khong hop le");
            return;
        }
        String auctionId = parts[1];
        double amount;
        try {
            amount = Double.parseDouble(parts[2]);
        } catch (NumberFormatException e) {
            out.println("BID_FAIL:So tien khong hop le");
            return;
        }

        Optional<User> userOpt = currentUser();
        if (userOpt.isEmpty()) {
            out.println("BID_FAIL:Khong tim thay tai khoan dang nhap");
            return;
        }
        User user = userOpt.get();
        if (!user.isActive()) {
            out.println("BID_FAIL:Tai khoan dang bi khoa");
            return;
        }
        if (!(user instanceof Bidder)) {
            out.println("BID_FAIL:Chi Bidder moi duoc dat gia");
            return;
        }
        if (user.getWalletBalance() < amount) {
            out.println("BID_FAIL:So du vi khong du. Vui long nap them tien");
            return;
        }

        AuctionManager.getInstance().findById(auctionId).ifPresentOrElse(
                auction -> {
                    try {
                        Bidder bidder = (Bidder) user;
                        auction.placeBid(bidder, amount);
                        out.println("BID_OK:" + amount);
                        DataStorage.saveUsers(UserManager.getInstance().getAllUsers());
                        DataStorage.saveAuctions(AuctionManager.getInstance().getAllAuctions());
                    } catch (InvalidBidException e) {
                        out.println("BID_FAIL:Gia phai cao hon " + auction.getCurrentHighestBid());
                    } catch (AuctionClosedException e) {
                        out.println("BID_FAIL:Phien da ket thuc!");
                    }
                },
                () -> out.println("BID_FAIL:Khong tim thay phien " + auctionId)
        );
    }

    private void sendAuctionList() {
        String list = AuctionManager.getInstance()
                .getVisibleAuctions()
                .stream()
                .map(this::auctionSummary)
                .collect(Collectors.joining(","));

        out.println("LIST_OK:" + (list.isEmpty() ? "Chua co phien nao" : list));
    }

    private void handleAddItem(String message) {
        Optional<User> userOpt = currentUser();
        if (userOpt.isEmpty() || !(userOpt.get() instanceof Seller)) {
            out.println("ADD_ITEM_FAIL:Chi Seller moi duoc dang san pham");
            return;
        }
        if (!userOpt.get().isActive()) {
            out.println("ADD_ITEM_FAIL:Tai khoan dang bi khoa");
            return;
        }

        String[] parts = message.split(":", 5);
        if (parts.length < 5) {
            out.println("ADD_ITEM_FAIL:Thieu tham so");
            return;
        }
        String name = parts[1], priceStr = parts[2], durStr = parts[3], category = parts[4];

        double startPrice; int duration;
        try {
            startPrice = Double.parseDouble(priceStr);
            duration   = Integer.parseInt(durStr);
            if (startPrice <= 0 || duration <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            out.println("ADD_ITEM_FAIL:Gia va thoi gian phai la so duong");
            return;
        }

        Item item;
        try {
            String itemId = "ITEM-" + System.currentTimeMillis() % 100000;
            item = ItemFactory.create(category.toUpperCase(), itemId, name, "", startPrice, "0");
        } catch (Exception e) {
            out.println("ADD_ITEM_FAIL:Danh muc khong hop le: " + category);
            return;
        }

        String auctionId = "A" + System.currentTimeMillis() % 100000;
        LocalDateTime endTime = LocalDateTime.now().plusSeconds(duration);
        Auction auction = new Auction(auctionId, item, endTime, clientName);
        auction.addObserver(new ServerBroadcastObserver());
        auction.startAuction();

        AuctionManager.getInstance().addAuction(auction);
        DataStorage.saveAuctions(AuctionManager.getInstance().getAllAuctions());

        out.println("ADD_ITEM_OK:" + auctionId);
        AuctionServer.broadcastAll("NEW_AUCTION:" + auctionId + ":" + name + ":" + startPrice + ":" + category + ":" + endTime);
        System.out.println("[Server] Phien moi: " + auctionId + " | " + name + " | " + startPrice + " | " + duration + "s");
    }

    private void handleWallet(String message) {
        Optional<User> userOpt = currentUser();
        if (userOpt.isEmpty()) {
            out.println("WALLET_FAIL:Khong tim thay tai khoan");
            return;
        }
        User user = userOpt.get();
        if (!user.isActive()) {
            out.println("WALLET_FAIL:Tai khoan dang bi khoa");
            return;
        }
        if (!user.supportsWallet()) {
            out.println("WALLET_HIDDEN:Vai tro nay khong su dung vi dau gia");
            return;
        }
        if ("WALLET:INFO".equals(message)) {
            sendWalletInfo();
            return;
        }
        String[] parts = message.split(":");
        if (parts.length < 3) {
            out.println("WALLET_FAIL:Thieu so tien");
            return;
        }
        try {
            double amount = Double.parseDouble(parts[2]);
            if (message.startsWith("WALLET:DEPOSIT:")) {
                user.deposit(amount);
                DataStorage.saveUsers(UserManager.getInstance().getAllUsers());
                out.println("WALLET_OK:Da nap " + amount + " VNĐ vao vi.");
            } else if (message.startsWith("WALLET:WITHDRAW:")) {
                user.withdraw(amount);
                DataStorage.saveUsers(UserManager.getInstance().getAllUsers());
                out.println("WALLET_OK:Da rut " + amount + " VNĐ khoi vi.");
            }
            sendWalletInfo();
        } catch (NumberFormatException e) {
            out.println("WALLET_FAIL:So tien khong hop le");
        } catch (IllegalArgumentException | IllegalStateException e) {
            out.println("WALLET_FAIL:" + e.getMessage());
        }
    }

    private void handleProfileUpdate(String message) {
        Optional<User> userOpt = currentUser();
        if (userOpt.isEmpty()) {
            out.println("PROFILE_FAIL:Khong tim thay tai khoan");
            return;
        }
        String[] parts = message.split(":", 5);
        String fullName = parts.length > 2 ? parts[2] : "";
        String phone = parts.length > 3 ? parts[3] : "";
        String password = parts.length > 4 ? parts[4] : "";
        User user = userOpt.get();
        user.setFullName(fullName);
        user.setPhoneNumber(phone);
        user.setPassword(password);
        DataStorage.saveUsers(UserManager.getInstance().getAllUsers());
        out.println("PROFILE_OK:" + user.getFullName() + ":" + user.getPhoneNumber());
    }

    private void handlePay(String message) {
        out.println("PAY_FAIL:Phiên thắng sẽ được hệ thống tự động trừ ví khi kết thúc, không cần thanh toán thủ công.");
    }

    private void sendWalletInfo() {
        currentUser().ifPresent(user -> {
            if (user.supportsWallet()) out.println("WALLET_INFO:" + user.getWalletBalance());
            else out.println("WALLET_HIDDEN:Vai tro nay khong su dung vi dau gia");
        });
    }

    private void handleMyAuctions() {
        Optional<User> userOpt = currentUser();
        if (userOpt.isEmpty() || !(userOpt.get() instanceof Seller)) {
            out.println("MY_AUCTIONS_FAIL:Chi Seller moi xem danh sach nay");
            return;
        }
        String list = AuctionManager.getInstance().findBySeller(clientName).stream()
                .map(this::auctionSummary)
                .collect(Collectors.joining(","));
        out.println("MY_AUCTIONS_OK:" + (list.isEmpty() ? "EMPTY" : list));
    }

    private void handleDetail(String message) {
        String[] parts = message.split(":", 2);
        if (parts.length < 2) {
            out.println("DETAIL_FAIL:Thieu ma phien");
            return;
        }
        AuctionManager.getInstance().findById(parts[1]).ifPresentOrElse(a -> {
            String sellerName = a.getSellerUsername() == null ? "" : a.getSellerUsername();
            String sellerPhone = UserManager.getInstance().findByUsername(sellerName)
                    .map(User::getPhoneNumber).orElse("");
            String leader = a.getCurrentLeader() == null ? "" : a.getCurrentLeader().getUsername();
            String history = a.getBidHistory().stream()
                    .map(tx -> {
                        String bidderName = tx.getBidder().getUsername();
                        String bidderPhone = UserManager.getInstance().findByUsername(bidderName).map(User::getPhoneNumber).orElse(tx.getBidder().getPhoneNumber());
                        return safe(bidderName) + "#" + tx.getAmount() + "#" + tx.getTimestamp() + "#" + safe(bidderPhone);
                    })
                    .collect(Collectors.joining(";"));
            out.println("DETAIL_OK:" + a.getAuctionId() + "|" + safe(a.getItem().getName()) + "|"
                    + a.getCurrentHighestBid() + "|" + a.getItem().getClass().getSimpleName().toUpperCase() + "|"
                    + a.getItem().getStartingPrice() + "|" + a.getEndTime() + "|" + a.getStatus() + "|"
                    + safe(sellerName) + "|" + safe(sellerPhone) + "|" + safe(leader) + "|" + history);
        }, () -> out.println("DETAIL_FAIL:Khong tim thay phien"));
    }

    private void handleMyBidderAuctions() {
        Optional<User> userOpt = currentUser();
        if (userOpt.isEmpty() || !(userOpt.get() instanceof Bidder)) {
            out.println("MY_BIDDER_AUCTIONS_FAIL:Chi Bidder moi xem danh sach nay");
            return;
        }
        String username = userOpt.get().getUsername();
        String joined = AuctionManager.getInstance().getAllAuctions().stream()
                .filter(a -> a.getBidHistory().stream().anyMatch(b -> username.equals(b.getBidder().getUsername())))
                .map(this::auctionSummary)
                .collect(Collectors.joining(","));
        String won = AuctionManager.getInstance().getAllAuctions().stream()
                .filter(a -> a.getCurrentLeader() != null && username.equals(a.getCurrentLeader().getUsername())
                        && (a.getStatus() == AuctionStatus.FINISHED || a.getStatus() == AuctionStatus.PAID))
                .map(this::auctionSummary)
                .collect(Collectors.joining(","));
        out.println("MY_BIDDER_AUCTIONS_OK:" + (joined.isEmpty() ? "EMPTY" : joined) + "::" + (won.isEmpty() ? "EMPTY" : won));
    }

    private void handleUserStatus(String message, boolean active) {
        String[] parts = message.split(":", 2);
        if (parts.length < 2 || parts[1].isBlank()) {
            out.println((active ? "UNLOCK_USER_FAIL:" : "LOCK_USER_FAIL:") + "Thieu ten tai khoan");
            return;
        }
        Optional<User> adminOpt = currentUser();
        if (adminOpt.isEmpty() || !(adminOpt.get() instanceof Admin)) {
            out.println((active ? "UNLOCK_USER_FAIL:" : "LOCK_USER_FAIL:") + "Chi Admin moi duoc thao tac");
            return;
        }
        String username = parts[1].trim();
        if (username.equals(adminOpt.get().getUsername())) {
            out.println((active ? "UNLOCK_USER_FAIL:" : "LOCK_USER_FAIL:") + "Khong the thay doi chinh tai khoan admin dang dang nhap");
            return;
        }
        boolean ok = active
                ? UserManager.getInstance().activateUser(username)
                : UserManager.getInstance().deactivateUser(username);
        if (ok) {
            DataStorage.saveUsers(UserManager.getInstance().getAllUsers());
            out.println((active ? "UNLOCK_USER_OK:" : "LOCK_USER_OK:") + username);
            AuctionServer.broadcastAll("USER_STATUS_CHANGED:" + username + ":" + (active ? "ACTIVE" : "LOCKED"));
        } else {
            out.println((active ? "UNLOCK_USER_FAIL:" : "LOCK_USER_FAIL:") + "Khong tim thay tai khoan");
        }
    }

    private void handleDeleteAuction(String message) {
        String[] parts = message.split(":", 2);
        if (parts.length < 2 || parts[1].isBlank()) {
            out.println("DELETE_AUCTION_FAIL:Thieu ma phien");
            return;
        }
        String auctionId = parts[1].trim();
        Optional<User> userOpt = currentUser();
        Optional<Auction> auctionOpt = AuctionManager.getInstance().findById(auctionId);
        if (userOpt.isEmpty() || auctionOpt.isEmpty()) {
            out.println("DELETE_AUCTION_FAIL:Khong tim thay tai khoan hoac phien");
            return;
        }
        User user = userOpt.get();
        Auction auction = auctionOpt.get();
        boolean allowed = user instanceof Admin || (user instanceof Seller && user.getUsername().equals(auction.getSellerUsername()));
        if (!allowed) {
            out.println("DELETE_AUCTION_FAIL:Ban khong co quyen xoa phien nay");
            return;
        }
        boolean removed = AuctionManager.getInstance().removeAuction(auctionId);
        if (removed) {
            DataStorage.saveAuctions(AuctionManager.getInstance().getAllAuctions());
            out.println("DELETE_AUCTION_OK:" + auctionId);
            AuctionServer.broadcastAll("AUCTION_DELETED:" + auctionId);
        } else {
            out.println("DELETE_AUCTION_FAIL:Xoa that bai");
        }
    }


    private void handleCancelAuction(String message) {
        String[] parts = message.split(":", 2);
        if (parts.length < 2 || parts[1].isBlank()) {
            out.println("CANCEL_AUCTION_FAIL:Thieu ma phien");
            return;
        }
        Optional<User> userOpt = currentUser();
        if (userOpt.isEmpty() || !(userOpt.get() instanceof Admin)) {
            out.println("CANCEL_AUCTION_FAIL:Chi Admin moi duoc huy phien");
            return;
        }
        String auctionId = parts[1].trim();
        boolean ok = AuctionManager.getInstance().cancelAuction(auctionId);
        if (ok) {
            DataStorage.saveAuctions(AuctionManager.getInstance().getAllAuctions());
            out.println("CANCEL_AUCTION_OK:" + auctionId);
            AuctionServer.broadcastAll("AUCTION_CANCELED:" + auctionId);
        } else {
            out.println("CANCEL_AUCTION_FAIL:Khong tim thay phien");
        }
    }

    private String auctionSummary(Auction a) {
        String leader = a.getCurrentLeader() == null ? "" : a.getCurrentLeader().getUsername();
        return a.getAuctionId() + "|" + safe(a.getItem().getName()) + "|" + a.getCurrentHighestBid() + "|"
                + a.getItem().getClass().getSimpleName().toUpperCase() + "|" + a.getEndTime() + "|"
                + a.getStatus() + "|" + safe(a.getSellerUsername()) + "|" + safe(leader) + "|"
                + a.getBidHistory().size();
    }

    private String safe(String value) {
        return value == null ? "" : value.replace("|", " ").replace(",", " ").replace(";", " ").replace("#", " ").replace(":", " ").trim();
    }

    public void sendMessage(String message) {
        if (out != null) out.println(message);
    }

    private void closeConnection() {
        try {
            if (in != null)     in.close();
            if (out != null)    out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.err.println("Loi dong ket noi: " + e.getMessage());
        }
    }

    public String getClientName() { return clientName; }
}
