package network;

import exception.AuctionClosedException;
import exception.InvalidBidException;
import model.Bidder;
import util.Singleton.AuctionManager;
import util.DataStorage;

import java.io.*;
import java.net.*;
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
            // Khởi tạo luồng đọc/ghi
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true); // autoFlush=true

            // Dòng đầu tiên client gửi là tên đăng nhập
            clientName = in.readLine();
            System.out.println(clientName + " da tham gia!");

            //  Gửi danh sách người đang online cho client mới
            String onlineList = AuctionServer.getConnectedClients()
                    .stream()
                    .filter(c -> c != this)           // không tính chính mình
                    .map(ClientHandler::getClientName)
                    .filter(name -> name != null)      // tránh null nếu client chưa gửi tên
                    .collect(Collectors.joining(", "));

            if (!onlineList.isEmpty()) {
                out.println("ONLINE:" + onlineList);
                // B nhận được: "ONLINE:alice, charlie"
            } else {
                out.println("ONLINE:Ban la nguoi dau tien trong phong!");
            }

            // Thông báo cho các client cũ biết có người mới vào
            AuctionServer.broadcast(clientName + " da tham gia phong!", this);

            // Vòng lặp đọc message từ client
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
            String[] parts    = message.split(":");
            String auctionId  = parts[1];
            double amount     = Double.parseDouble(parts[2]);

            AuctionManager.getInstance().findById(auctionId).ifPresentOrElse(
                    auction -> {
                        try {
                            Bidder bidder = new Bidder(clientName, clientName, "");
                            auction.placeBid(bidder, amount);

                            // Broadcast giá mới cho tất cả client
                            AuctionServer.broadcast(
                                    "GIA_MOI:" + auctionId + ":" + amount + ":" + clientName,
                                    this);
                            out.println("BID_OK:" + amount);

                            // Lưu dữ liệu sau mỗi bid thành công
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
            // Trả về danh sách phiên thực từ AuctionManager
            String list = AuctionManager.getInstance().getAllAuctions()
                    .stream()
                    .map(a -> a.getAuctionId() + "|" + a.getItem().getName()
                            + "|" + a.getCurrentHighestBid()
                            + "|" + a.getStatus())
                    .collect(Collectors.joining(","));

            out.println("LIST_OK:" + (list.isEmpty() ? "Chua co phien nao" : list));

        } else {
            out.println("UNKNOWN_COMMAND");
        }
    }

    // Gửi message đến client này
    public void sendMessage(String message) {
        if (out != null) out.println(message);
    }

    private void closeConnection() {
        try {
            if (in     != null) in.close();
            if (out    != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.err.println("Loi dong ket noi: " + e.getMessage());
        }
    }

    public String getClientName() { return clientName; }
}