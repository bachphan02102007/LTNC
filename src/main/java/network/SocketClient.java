package network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.function.Consumer;
import javafx.application.Platform;


public class SocketClient {

    private static final String HOST = "localhost";
    private static final int PORT = 9999;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private volatile Consumer<String> onMessage;
    private volatile boolean running = false;


    public void connect(String username, Consumer<String> onMessage) throws IOException {
        connect(username, null, onMessage);
    }

    public void connect(String username, String role, Consumer<String> onMessage) throws IOException {
        this.onMessage = onMessage;
        socket = new Socket(HOST, PORT);
        out = new PrintWriter(socket.getOutputStream(), true);
        in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out.println(role == null || role.isBlank() ? username : username + "|" + role);
        running = true;
        startListenerThread();
    }

    private void startListenerThread() {
        Thread t = new Thread(() -> {
            try {
                String line;
                while (running && (line = in.readLine()) != null) {
                    final String msg = line;
                    // Luôn dispatch lên JavaFX Application Thread
                    Platform.runLater(() -> {
                        if (onMessage != null) onMessage.accept(msg);
                    });
                }
            } catch (IOException e) {
                if (running) {
                    Platform.runLater(() -> {
                        if (onMessage != null)
                            onMessage.accept("ERROR:Mat ket noi toi server");
                    });
                }
            }
        });
        t.setDaemon(true);
        t.setName("SocketClient-Listener");
        t.start();
    }


     // Cập nhật callback xử lý message — dùng khi chuyển sang màn hình khác
     //nhưng vẫn giữ cùng 1 socket connection.
     // Ví dụ: AuctionList → AuctionRoom đều dùng chung socket,
     // chỉ cần gọi setOnMessage() để đổi handler.

    public void setOnMessage(Consumer<String> onMessage) {
        this.onMessage = onMessage;
    }

    public void sendBid(String auctionId, double amount) {
        sendCommand("BID:" + auctionId + ":" + amount);
    }

    public void requestList() {
        sendCommand("LIST");
    }

    public void sendAddItem(String name, double startPrice, int durationSec, String category) {
        sendCommand("ADD_ITEM:" + name + ":" + startPrice + ":" + durationSec + ":" + category);
    }

    public void requestWallet() {
        sendCommand("WALLET:INFO");
    }

    public void deposit(double amount) {
        sendCommand("WALLET:DEPOSIT:" + amount);
    }

    public void withdraw(double amount) {
        sendCommand("WALLET:WITHDRAW:" + amount);
    }

    public void updateProfile(String fullName, String phone, String newPassword) {
        sendCommand("PROFILE:UPDATE:" + safe(fullName) + ":" + safe(phone) + ":" + safe(newPassword));
    }

    public void requestDetail(String auctionId) {
        sendCommand("DETAIL:" + safe(auctionId));
    }

    public void requestMyAuctions() {
        sendCommand("MY_AUCTIONS");
    }

    public void sendPay(String auctionId) {
        sendCommand("PAY:" + auctionId);
    }

    private String safe(String value) {
        return value == null ? "" : value.replace(":", " ").trim();
    }

    public void sendCommand(String command) {
        if (out != null && socket != null && !socket.isClosed()) {
            out.println(command);
        }
    }

    public void disconnect() {
        running = false;
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) { }
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }
}
