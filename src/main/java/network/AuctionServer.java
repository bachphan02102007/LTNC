package network;

import exception.AuthenticationException;
import model.Auction;
import model.User;
import util.Singleton.AuctionManager;
import util.DataStorage;
import util.Singleton.UserManager;

import java.io.*;
import java.net.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class AuctionServer {

    private static final int PORT = 9999;

    // Danh sách tất cả ClientHandler đang kết nối
    // CopyOnWriteArrayList — thread-safe, nhiều thread đọc/ghi cùng lúc không bị lỗi
    private static final List<ClientHandler> connectedClients =
            new CopyOnWriteArrayList<>();

    public static void main(String[] args) throws IOException {
        System.out.println("Server dang khoi dong tren cong " + PORT + "...");

        //tải dữ liệu cux
        List<User> savedUsers = DataStorage.loadUsers();
        savedUsers.forEach(u -> {
            try {
                UserManager.getInstance().addUser(u);
            } catch (AuthenticationException ignored) {}
        });
        System.out.println("Da tai " + savedUsers.size() + " users tu file.");

        List<Auction> savedAuctions = DataStorage.loadAuctions();
        savedAuctions.forEach(a -> AuctionManager.getInstance().addAuction(a));
        System.out.println("Da tai " + savedAuctions.size() + " phien dau gia tu file.");

        // lưu dữ liệu khi server tắt
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Server dang tat, luu du lieu...");
            DataStorage.saveUsers(UserManager.getInstance().getAllUsers());
            DataStorage.saveAuctions(AuctionManager.getInstance().getAllAuctions());
            System.out.println("Da luu du lieu thanh cong!");
        }));

        // ThreadPool: tối đa 20 client cùng lúc
        ExecutorService pool = Executors.newFixedThreadPool(20);

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

    // Trả về danh sách client đang online — dùng để thông báo cho client mới vào
    public static List<ClientHandler> getConnectedClients() {
        return connectedClients;
    }

    // Xóa client khỏi danh sách khi họ ngắt kết nối
    public static void removeClient(ClientHandler handler) {
        connectedClients.remove(handler);
        System.out.println("Client ngat ket noi. Con lai: "
                + connectedClients.size() + " clients.");
        // Thông báo cho các client còn lại biết có người vừa thoát
        broadcast(handler.getClientName() + " da roi phong!", handler);
    }
}