package util;

import model.User;
import network.SocketClient;

/**
 * Singleton giữ thông tin session hiện tại (user + socket).
 * Thay vì truyền User qua từng màn hình, các Controller lấy từ đây.
 */
public class SessionManager {

    private static SessionManager instance;

    private User currentUser;
    private SocketClient socketClient;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void login(User user) {
        this.currentUser = user;
        // Tạo SocketClient mới mỗi lần login
        this.socketClient = new SocketClient();
    }

    public void logout() {
        if (socketClient != null) socketClient.disconnect();
        currentUser = null;
        socketClient = null;
    }

    public User getCurrentUser() { return currentUser; }
    public SocketClient getSocketClient() { return socketClient; }
}
