package test;

import exception.AuthenticationException;
import model.Bidder;
import model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import util.Singleton.UserManager;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class UserManagerTest {

    private UserManager userManager;

    @BeforeEach
    void setUp() {
        userManager = UserManager.getInstance();
        // Xóa toàn bộ dữ liệu cũ để đảm bảo mỗi bài test chạy hoàn toàn độc lập
        userManager.clearAll();
    }

    // Test 1: Kiểm tra việc thêm một tài khoản mới vào hệ thống thành công.
    @Test
    void testAddUserSuccess() throws Exception {
        // Bước 1: Chuẩn bị thông tin một người dùng mới
        User user = new Bidder("1", "thanh", "123", "090");

        // Bước 2: Thực hiện thêm người dùng vào hệ thống quản lý
        userManager.addUser(user);

        // Bước 3: Kiểm tra xem hệ thống đã lưu đúng tài khoản đó chưa
        Optional<User> result = userManager.findByUsername("thanh");
        assertTrue(result.isPresent(), "Lỗi: Không tìm thấy người dùng sau khi đã thêm!");
        assertEquals("thanh", result.get().getUsername());
    }

    // Test 2: Chặn không cho phép hai tài khoản trùng tên đăng ký vào hệ thống.
    @Test
    void testAddDuplicateUser() throws Exception {
        // Bước 1: Thêm trước một tài khoản tên là "duy"
        userManager.addUser(new Bidder("1", "duy", "123", "090"));

        // Bước 2: Cố tình thêm tiếp một tài khoản khác nhưng cũng tên là "duy"
        // Hệ thống bắt buộc phải ném ra lỗi AuthenticationException
        assertThrows(AuthenticationException.class, () -> {
            userManager.addUser(new Bidder("2", "duy", "999", "091"));
        });

        // Bước 3: Kiểm tra lại để đảm bảo tài khoản "duy" đầu tiên không bị thay đổi mật khẩu thành "999"
        Optional<User> existingUser = userManager.findByUsername("duy");
        assertEquals("123", existingUser.get().getPassword(), "Lỗi: Tài khoản cũ bị ghi đè dữ liệu!");
    }

    // Test 3: Xác thực đăng nhập thành công khi người dùng nhập đúng tên và đúng mật khẩu.
    @Test
    void testLoginSuccess() throws Exception {
        // Bước 1: Tạo sẵn một tài khoản trong hệ thống
        userManager.addUser(new Bidder("1", "duy", "123", "090"));

        // Bước 2: Thực hiện đăng nhập đúng tên và đúng mật khẩu
        Optional<User> user = userManager.authenticate("duy", "123");

        // Bước 3: Kiểm tra xem hệ thống có trả về thông tin người dùng hay không
        assertTrue(user.isPresent(), "Lỗi: Đăng nhập thất bại dù nhập đúng thông tin!");
        assertEquals("duy", user.get().getUsername());
    }

    // Test 4: Từ chối đăng nhập khi người dùng nhập đúng tên nhưng sai mật khẩu.
    @Test
    void testLoginFailWrongPassword() throws Exception {
        // Bước 1: Tạo sẵn một tài khoản trong hệ thống
        userManager.addUser(new Bidder("1", "duy", "123", "090"));

        // Bước 2: Thực hiện đăng nhập với mật khẩu sai ("wrong")
        Optional<User> user = userManager.authenticate("duy", "wrong");

        // Bước 3: Hệ thống không được phép đăng nhập thành công (kết quả trả về phải rỗng)
        assertTrue(user.isEmpty(), "Lỗi: Hệ thống vẫn cho đăng nhập khi sai mật khẩu!");
    }

    // Test 5: Từ chối đăng nhập khi người dùng nhập một tài khoản không hề tồn tại.
    @Test
    void testLoginFailNonExistentUser() throws Exception {
        // Bước 1: Hệ thống hiện tại hoàn toàn trống rỗng, không có tài khoản nào tên "stranger"

        // Bước 2: Thực hiện cố gắng đăng nhập bằng tài khoản không tồn tại
        Optional<User> user = userManager.authenticate("stranger", "123");

        // Bước 3: Hệ thống phải báo đăng nhập thất bại (kết quả trả về rỗng)
        assertTrue(user.isEmpty(), "Lỗi: Hệ thống cho phép đăng nhập bằng tài khoản không tồn tại!");
    }
}