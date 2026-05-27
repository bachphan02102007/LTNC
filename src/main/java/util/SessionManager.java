package util;

//Vai trò của SessionManager
//SessionManager không quản lý tất cả user, mà chỉ giữ user hiện tại đang đăng nhập.
//Nó lưu:
//currentUser → đối tượng user đã đăng nhập.
//socketClient → kết nối mạng realtime gắn với user đó.
//👉 Nó giống như “thông tin phiên làm việc” của người dùng hiện tại.

import model.User;
import network.SocketClient;


 //Singleton giữ thông tin session hiện tại (user + socket).
 //Thay vì truyền User qua từng màn hình, các Controller lấy từ đây.

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
        this.socketClient = new SocketClient();
    }

    public void logout() {
        if (socketClient != null) socketClient.disconnect();
        currentUser = null;
        socketClient = null;
    }

    public User getCurrentUser() { return currentUser; }
    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }
    public SocketClient getSocketClient() { return socketClient; }
}
