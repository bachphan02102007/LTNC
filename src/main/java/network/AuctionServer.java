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

    private static final List<ClientHandler> connectedClients =
            new CopyOnWriteArrayList<>();

    public static void main(String[] args) throws IOException {
        System.out.println("Server dang khoi dong tren cong " + PORT + "...");

        List<User> savedUsers = DataStorage.loadUsers();
        savedUsers.forEach(u -> {
            try { UserManager.getInstance().addUser(u); }
            catch (AuthenticationException ignored) {}
        });
        System.out.println("Da tai " + savedUsers.size() + " users tu file.");

        List<Auction> savedAuctions = DataStorage.loadAuctions();
        savedAuctions.forEach(a -> AuctionManager.getInstance().addAuction(a));
        System.out.println("Da tai " + savedAuctions.size() + " phien dau gia tu file.");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Server dang tat, luu du lieu...");
            DataStorage.saveUsers(UserManager.getInstance().getAllUsers());
            DataStorage.saveAuctions(AuctionManager.getInstance().getAllAuctions());
            System.out.println("Da luu du lieu thanh cong!");
        }));

        ExecutorService pool = Executors.newFixedThreadPool(20);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) { //"Mở một cánh cửa để client kết nối vào, sau khi mở nó sẽ ngồi chờ"
            System.out.println("Server san sang nhan ket noi!");// nó là 1 class có sẵn trong java có chức năng lắng nghe

            while (true) {
                // nódđợi 1 client kết nối tới , đây là đường dây liên lạc giữa client và server.
                //mỗi lần có client nó sẽ tạo ra 1 socket mới và chỉ phục vụ cho client đó.
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client moi ket noi: " + clientSocket.getInetAddress());


                // và sau khi tạo xong socket cho client đó thì sẽ taoj ra 1 clientHandler riêng để dữ nó
                //handler A dữ socket A

                ClientHandler handler = new ClientHandler(clientSocket); // clientsocket được truyền vào để handler biết thằng nào để xử lý
                connectedClients.add(handler); // mục đích biết ai đang kết nối để gửi thông báo hoặc broadcast
                pool.execute(handler); // đa luồng thực hiện cac clientHandler
            }
        }
    }

    /**
     * Broadcast đến tất cả client NGOẠI TRỪ sender.
     * Dùng cho: GIA_MOI, thông báo join/leave.
     */
    public static void broadcast(String message, ClientHandler sender) {
        for (ClientHandler client : connectedClients) {
            if (client != sender) {
                client.sendMessage(message);
            }
        }
    }

    /**
     * Broadcast đến TẤT CẢ client kể cả người gửi.
     * Dùng cho: NEW_AUCTION — cả Seller cũng cần cập nhật list.
     */
    public static void broadcastAll(String message) {
        for (ClientHandler client : connectedClients) {
            client.sendMessage(message);
        }
    }

    public static List<ClientHandler> getConnectedClients() {
        return connectedClients;
    }

    public static void removeClient(ClientHandler handler) {
        connectedClients.remove(handler);
        System.out.println("Client ngat ket noi. Con lai: "
                + connectedClients.size() + " clients.");
        broadcast(handler.getClientName() + " da roi phong!", handler);
    }
}
