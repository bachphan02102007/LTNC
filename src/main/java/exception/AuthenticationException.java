package exception;

// Lỗi khi đăng nhập sai tên/mật khẩu hoặc không có quyền
public class AuthenticationException extends Exception {
    public AuthenticationException(String message) {
        super(message);
    }
}
