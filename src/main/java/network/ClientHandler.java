package network;

import java.io.*;
import java.net.*;

// Mỗi client kết nối sẽ có 1 ClientHandler (trình xử ký khách hàng) riêng chạy trên 1 Thread riêng
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
            AuctionServer.broadcast(clientName + " da tham gia phong!", this);

            // Vòng lặp đọc message từ client
            String message;
            while ((message = in.readLine()) != null) {
                System.out.println("[" + clientName + "]: " + message);

                // Xử lý lệnh từ client
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
        // Format: "BID:A001:650.0" — client đặt giá
        if (message.startsWith("BID:")) {
            String[] parts = message.split(":");
            String auctionId = parts[1];
            double amount    = Double.parseDouble(parts[2]);

            // rồi gọi placeBid() và broadcast kết quả về cho tất cả client
            AuctionServer.broadcast(
                    "GIA_MOI:" + auctionId + ":" + amount + ":" + clientName, this);
            out.println("BID_OK:" + amount);

        } else if (message.equals("LIST")) {
            out.println("LIST_OK:A001,A002");

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
            if (in  != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.err.println("Loi dong ket noi: " + e.getMessage());
        }
    }

    public String getClientName() { return clientName; }
}