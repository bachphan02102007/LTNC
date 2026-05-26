package test;

import exception.AuthenticationException;
import model.Bidder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import util.Singleton.UserManager;

import static org.junit.jupiter.api.Assertions.*;

class RegisterLogicTest {

    private UserManager um;

    @BeforeEach
    void setUp() {
        um = UserManager.getInstance();
        // Xóa dữ liệu cũ trước khi chạy mỗi test
        // để đảm bảo các bài test không bị dính dữ liệu của nhau.
        um.clearAll();
    }

    // Test 1: Kiểm tra luồng đăng ký một tài khoản hoàn toàn hợp lệ.
    @Test
    void testRegisterSuccess() throws Exception {
        // Bước 1: Chuẩn bị thông tin người dùng mới
        Bidder newBidder = new Bidder("1", "duc", "123", "090");

        // Bước 2: Gọi logic đăng ký
        um.addUser(newBidder);

        // Bước 3: Xác nhận tài khoản đã được lưu thành công vào hệ thống
        assertTrue(
                um.findByUsername("duc").isPresent(),
                "Lỗi: Quá trình đăng ký báo thành công nhưng không tìm thấy dữ liệu!"
        );
    }

    // Test 2: Đảm bảo logic đăng ký chặn chặn đứng việc tạo 2 tài khoản trùng tên.
    @Test
    void testRegisterDuplicate() throws Exception {
        // Bước 1: Đăng ký trước một tài khoản tên "duy"
        um.addUser(new Bidder("1", "duy", "123", "090"));

        // Bước 2: Cố tình đăng ký một tài khoản khác cũng dùng tên "duy"
        // Kỳ vọng: Luồng logic phải ném ra ngoại lệ báo lỗi
        assertThrows(
                AuthenticationException.class,
                () -> um.addUser(new Bidder("2", "duy", "999", "091")),
                "Lỗi: Hệ thống không chặn logic đăng ký trùng username!"
        );
    }

    // Test 3: Kiểm tra xác thực thông tin (Logic Login)
    // đảm bảo tính toàn vẹn của mật khẩu sau khi đăng ký.
    @Test
    void testLoginLogic() throws Exception {
        // Bước 1: Tạo một tài khoản chuẩn
        um.addUser(new Bidder("1", "duythanh", "123", "090"));

        // Bước 2: Kiểm tra kịch bản đăng nhập ĐÚNG mật khẩu
        assertTrue(
                um.authenticate("duythanh", "123").isPresent(),
                "Lỗi: Logic từ chối đăng nhập dù nhập đúng tài khoản và mật khẩu!"
        );

        // Bước 3: Kiểm tra kịch bản đăng nhập SAI mật khẩu
        assertTrue(
                um.authenticate("duy", "wrong").isEmpty(),
                "Lỗi: Logic bảo mật yếu, cho phép đăng nhập khi sai mật khẩu!"
        );
    }
}