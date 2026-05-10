package network;

import exception.AuctionClosedException;
import exception.InvalidBidException;
import model.*;
import util.Factory.ItemFactory;
import util.Singleton.AuctionManager;
import util.DataStorage;

import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String clientName;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            clientName = in.readLine();
            System.out.println(clientName + " da tham gia!");

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

        if (message.startsWith("BID:")) {
            String[] parts   = message.split(":");
            String auctionId = parts[1];
            double amount    = Double.parseDouble(parts[2]);

            AuctionManager.getInstance().findById(auctionId).ifPresentOrElse(
                    auction -> {
                        try {
                            Bidder bidder = new Bidder("", clientName, "");
                            auction.placeBid(bidder, amount);
                            // BID_OK gửi riêng cho người bid
                            out.println("BID_OK:" + amount);
                            // GIA_MOI đã được broadcast qua ServerBroadcastObserver.onBidUpdated()
                            DataStorage.saveAuctions(
                                    AuctionManager.getInstance().getAllAuctions());
                        } catch (InvalidBidException e) {
                            out.println("BID_FAIL:Gia phai cao hon "
                                    + auction.getCurrentHighestBid());
                        } catch (AuctionClosedException e) {
                            out.println("BID_FAIL:Phien da ket thuc!");
                        }
                    },
                    () -> out.println("BID_FAIL:Khong tim thay phien " + auctionId)
            );

        } else if (message.equals("LIST")) {
            String list = AuctionManager.getInstance().getAllAuctions().stream()
                    .map(a -> a.getAuctionId() + "|" + a.getItem().getName()
                            + "|" + a.getCurrentHighestBid() + "|" + a.getStatus())
                    .collect(Collectors.joining(","));
            out.println("LIST_OK:" + (list.isEmpty() ? "Chua co phien nao" : list));

        } else if (message.startsWith("ADD_ITEM:")) {
            handleAddItem(message);

        } else {
            out.println("UNKNOWN_COMMAND");
        }
    }

    private void handleAddItem(String message) {
        String[] parts = message.split(":", 5);
        if (parts.length < 5) {
            out.println("ADD_ITEM_FAIL:Thieu tham so");
            return;
        }
        String name = parts[1], priceStr = parts[2],
                durStr = parts[3], category = parts[4];

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
            item = ItemFactory.create(
                    category.toUpperCase(), itemId, name, "", startPrice, clientName);
        } catch (Exception e) {
            out.println("ADD_ITEM_FAIL:Danh muc khong hop le: " + category);
            return;
        }

        String auctionId = "A" + System.currentTimeMillis() % 100000;
        LocalDateTime endTime = LocalDateTime.now().plusSeconds(duration);
        Auction auction = new Auction(auctionId, item, endTime);

        // ★ Gắn observer TRƯỚC startAuction — nhờ đây AUCTION_CLOSED tự broadcast
        auction.addObserver(new ServerBroadcastObserver());
        auction.startAuction();

        AuctionManager.getInstance().addAuction(auction);
        DataStorage.saveAuctions(AuctionManager.getInstance().getAllAuctions());

        out.println("ADD_ITEM_OK:" + auctionId);
        AuctionServer.broadcastAll("NEW_AUCTION:" + auctionId + ":"
                + name + ":" + startPrice + ":" + category);

        System.out.println("[Server] Phien moi: " + auctionId
                + " | " + name + " | " + startPrice + " | " + duration + "s");
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
