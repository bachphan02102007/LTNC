package test;

import model.Bidder;
import model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import util.SessionManager;

import static org.junit.jupiter.api.Assertions.*;

class SessionManagerTest {

    private SessionManager session;

    @BeforeEach
    void setUp() {
        // Lấy thực thể quản lý phiên (session) duy nhất của hệ thống
        session = SessionManager.getInstance();

        // Bước dọn dẹp: Đăng xuất mọi tài khoản trước khi chạy test
        // để đảm bảo bài test này không bị ảnh hưởng bởi bài test trước đó
        session.logout();
    }

    // Test 1: Đảm bảo khi hệ thống vừa khởi động, mặc định không có ai đang đăng nhập.
    @Test
    void testInitialState_ShouldBeNull() {
        // Bước 1 & 2: Không làm gì cả, chỉ lấy trạng thái hiện tại
        // Bước 3: Kỳ vọng hệ thống chưa ghi nhận bất kỳ người dùng nào
        assertNull(
                session.getCurrentUser(),
                "Lỗi: Hệ thống vừa khởi động mà đã có phiên đăng nhập tồn tại!"
        );
    }

    // Test 2: Kiểm tra hệ thống có lưu đúng thông tin người dùng khi họ đăng nhập hay không.
    @Test
    void testLoginShouldStoreSession() {
        // Bước 1: Chuẩn bị một tài khoản người dùng hợp lệ
        User user = new Bidder("U001", "alice", "123");

        // Bước 2: Thực hiện hành động đăng nhập
        session.login(user);

        // Bước 3: Kiểm tra xem phiên làm việc có lưu đúng người này không
        assertNotNull(session.getCurrentUser(), "Lỗi: Phiên đăng nhập không được tạo!");
        assertEquals("alice", session.getCurrentUser().getUsername());
        assertEquals(user, session.getCurrentUser(), "Lỗi: Dữ liệu người dùng trong phiên bị sai lệch!");
    }

    // Test 3: Đảm bảo khi người dùng đăng xuất, toàn bộ thông tin phiên làm việc phải bị xóa sạch.
    @Test
    void testLogoutShouldClearSession() {
        // Bước 1: Cho một người dùng đăng nhập vào hệ thống trước
        User user = new Bidder("U002", "bob", "456");
        session.login(user);

        // Bước 2: Thực hiện hành động đăng xuất
        session.logout();

        // Bước 3: Kiểm tra hệ thống, đảm bảo không còn ai đang đăng nhập nữa
        assertNull(
                session.getCurrentUser(),
                "Lỗi: Đăng xuất rồi nhưng thông tin người dùng vẫn còn kẹt lại trong hệ thống!"
        );
    }

    // Test 4: Tránh lỗi bảo mật (Security Issue) - Đảm bảo khi một người đăng xuất và người khác đăng nhập,
    // thông tin của người cũ không bị dính sang người mới.
    @Test
    void testSessionShouldNotBeReusedAfterLogout() {
        // Bước 1: Alice đăng nhập rồi đăng xuất
        User user1 = new Bidder("U003", "alice", "123");
        session.login(user1);
        session.logout();

        // Bước 2: Bob dùng chung thiết bị/ứng dụng đó để đăng nhập
        User user2 = new Bidder("U004", "bob", "456");
        session.login(user2);

        // Bước 3: Hệ thống phải nhận diện 100% đây là Bob, không còn sót lại dấu vết nào của Alice
        assertEquals("bob", session.getCurrentUser().getUsername(), "Lỗi: Không nhận diện đúng người đăng nhập sau!");
        assertNotEquals("alice", session.getCurrentUser().getUsername(), "Lỗi bảo mật: Dữ liệu người dùng cũ bị lộ sang phiên mới!");
    }
}