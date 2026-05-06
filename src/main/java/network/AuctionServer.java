package network;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;


public class AuctionServer {

    //PORT chính là cổng mạng mà server mở ra để chờ client kết nối.
    private static final int PORT = 9999;

    // Danh sách tất cả ClientHandler đang kết nối
    // CopyOnWriteArrayList — thread-safe, nhiều thread đọc/ghi cùng lúc không bị lỗi
    private static final List<ClientHandler> connectedClients =
            new CopyOnWriteArrayList<>();
    public static void main(String[] args) throws IOException {
        System.out.println("Server dang khoi dong tren cong " + PORT + "...");

        // ThreadPool: tối đa 100 client cùng lúc
        ExecutorService pool = Executors.newFixedThreadPool(100);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server san sang nhan ket noi!");

            while (true) {
                // Chờ client kết nối — blocking call
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client moi ket noi: "
                        + clientSocket.getInetAddress());

                // Tạo handler riêng cho client này và chạy trên thread riêng
                ClientHandler handler = new ClientHandler(clientSocket);
                connectedClients.add(handler);
                pool.execute(handler);
            }
        }
    }

    // Gửi message đến TẤT CẢ client đang kết nối — dùng cho Observer notify
    public static void broadcast(String message, ClientHandler sender) {
        for (ClientHandler client : connectedClients) {
            if (client != sender) { // không gửi lại cho người vừa gửi
                client.sendMessage(message);
            }
        }
    }

    // Xóa client khỏi danh sách khi họ ngắt kết nối
    public static void removeClient(ClientHandler handler) {
        connectedClients.remove(handler);
        System.out.println("Client ngat ket noi. Con lai: "
                + connectedClients.size() + " clients.");
    }
}
