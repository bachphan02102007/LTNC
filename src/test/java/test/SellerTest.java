package test;

import model.Seller;
import model.User;
import model.WalletOwner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;

import static org.junit.jupiter.api.Assertions.*;

class SellerTest {

    private Seller seller;

    // Chạy trước mỗi test case để đảm bảo luôn có một đối tượng seller mẫu, mới hoàn toàn
    @BeforeEach
    void setUp() {
        seller = new Seller(
                "S001",
                "seller123",
                "123456",
                "0988888888"
        );
    }

    // Test 1: Đảm bảo các thuộc tính thiếu được gán giá trị mặc định chuẩn (chuỗi rỗng, số dư = 0) khi tạo Seller không có số điện thoại.
    @Test
    void testConstructorWithoutPhoneNumber() {
        // Bước 1: Khởi tạo đối tượng Seller mới chỉ với các thông tin cơ bản
        Seller s = new Seller("S002", "testSeller", "password");

        // Bước 2: Xác nhận hệ thống gán chính xác các thông tin cơ bản
        assertEquals("S002", s.getId());
        assertEquals("testSeller", s.getUsername());
        assertEquals("password", s.getPassword());
        // Bước 3: Kiểm tra các giá trị mặc định được tự động thiết lập
        assertEquals("", s.getPhoneNumber()); // Mặc định là chuỗi rỗng
        assertEquals(0, s.getWalletBalance());
        assertTrue(s.isActive());
    }

    // Test 2: Đảm bảo constructor nhận và lưu trữ chính xác toàn bộ dữ liệu truyền vào khi tạo Seller có đầy đủ thông tin.
    @Test
    void testConstructorWithPhoneNumber() {
        // Bước 1: Kiểm tra đối tượng 'seller' từ hàm setUp() xem có lưu đúng dữ liệu khởi tạo không
        assertEquals("S001", seller.getId());
        assertEquals("seller123", seller.getUsername());
        assertEquals("123456", seller.getPassword());
        assertEquals("0988888888", seller.getPhoneNumber());
        // Bước 2: Xác nhận họ tên được gán mặc định bằng với username
        assertEquals("seller123", seller.getFullName()); // Họ tên mặc định bằng username
        // Bước 3: Kiểm tra số dư mặc định và trạng thái
        assertEquals(0, seller.getWalletBalance());
        assertTrue(seller.isActive());
    }

    // Test 3: Xác nhận Seller được cấp quyền sử dụng ví tiền (thông qua interface).
    @Test
    void testSupportsWallet() {
        // Bước 1 & 2: Gọi hàm kiểm tra và kỳ vọng kết quả trả về là true
        assertTrue(seller.supportsWallet());
    }

    // Test 4: Kiểm tra việc cập nhật số dư ví với một giá trị dương hợp lệ.
    @Test
    void testSetWalletBalance() {
        // Bước 1: Cập nhật số dư ví thành 500
        seller.setWalletBalance(500);
        // Bước 2: Kiểm tra số dư ví đã thay đổi chính xác
        assertEquals(500, seller.getWalletBalance());
    }

    // Test 5: Đảm bảo hệ thống chặn việc set số dư âm và ném ra đúng lỗi ngoại lệ.
    @Test
    void testSetWalletBalanceNegative() {
        // Bước 1: Cố tình gán số dư ví bằng một số âm
        // Bước 2: Bắt lỗi IllegalArgumentException và xác nhận thông báo lỗi chính xác
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> seller.setWalletBalance(-100));
        assertEquals("So du vi khong duoc am", exception.getMessage());
    }

    // Test 6: Nạp tiền hợp lệ, đảm bảo số dư ví tăng đúng bằng số tiền được nạp vào.
    @Test
    void testDeposit() {
        // Bước 1: Nạp 300 vào ví
        seller.deposit(300);
        // Bước 2: Xác nhận số dư tăng lên đúng mốc 300
        assertEquals(300, seller.getWalletBalance());
    }

    // Test 7: Chặn và báo lỗi nếu người dùng nhập số tiền nạp là số âm.
    @Test
    void testDepositNegative() {
        // Bước 1 & 2: Cố tình nạp số tiền âm và xác nhận hệ thống ném ra ngoại lệ
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> seller.deposit(-50));
        assertEquals("So tien nap phai lon hon 0", exception.getMessage());
    }

    // Test 8: Đảm bảo số tiền nạp vào hệ thống bắt buộc phải thực sự lớn hơn 0 (không chấp nhận 0).
    @Test
    void testDepositZero() {
        // Bước 1 & 2: Nạp đúng 0 đồng và đảm bảo hệ thống chặn lại bằng ngoại lệ
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> seller.deposit(0));
        assertEquals("So tien nap phai lon hon 0", exception.getMessage());
    }

    // Test 9: Rút số tiền nhỏ hơn số dư hiện tại, đảm bảo tài khoản bị trừ đúng số tiền đã rút.
    @Test
    void testWithdraw() {
        // Bước 1: Nạp 500 làm số dư ban đầu
        seller.deposit(500);
        // Bước 2: Rút 200 ra khỏi ví
        seller.withdraw(200);
        // Bước 3: Xác nhận số dư còn lại chính xác là 300
        assertEquals(300, seller.getWalletBalance());
    }

    // est 10: Đảm bảo hệ thống cho phép rút hết sạch tiền trong ví và số dư về mức 0.
    @Test
    void testWithdrawAll() {
        // Bước 1: Nạp 500 làm số dư
        seller.deposit(500);
        // Bước 2: Rút toàn bộ 500
        seller.withdraw(500);
        // Bước 3: Kiểm tra số dư ví về đúng 0
        assertEquals(0, seller.getWalletBalance());
    }

    // Test 11: Ngăn chặn ngoại lệ khi cố tình rút một số tiền âm.
    @Test
    void testWithdrawNegative() {
        // Bước 1 & 2: Rút số tiền âm và kiểm tra hệ thống có ném lỗi chặn lại không
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> seller.withdraw(-10));
        assertEquals("So tien rut phai lon hon 0", exception.getMessage());
    }

    // Test 12: Chặn thao tác rút tiền với số tiền truyền vào bằng đúng 0.
    @Test
    void testWithdrawZero() {
        // Bước 1 & 2: Thử rút 0 đồng và kiểm tra thông báo lỗi
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> seller.withdraw(0));
        assertEquals("So tien rut phai lon hon 0", exception.getMessage());
    }

    // Test 13: Ngăn chặn tuyệt đối việc tài khoản người bán bị rút âm tiền (rút nhiều hơn số dư).
    @Test
    void testWithdrawInsufficientBalance() {
        // Bước 1: Thiết lập số dư ví là 100
        seller.deposit(100);
        // Bước 2 & 3: Rút 200 (vượt số dư) và kiểm tra ngoại lệ thông báo không đủ tiền
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> seller.withdraw(200));
        assertEquals("So du vi khong du", exception.getMessage());
    }

    // Test 14: Kiểm tra việc cập nhật mật khẩu mới với dữ liệu hợp lệ.
    @Test
    void testSetPassword() {
        // Bước 1: Cập nhật mật khẩu mới
        seller.setPassword("newpass");
        // Bước 2: Xác nhận mật khẩu đã thay đổi
        assertEquals("newpass", seller.getPassword());
    }

    // Test 15: Đảm bảo hệ thống giữ nguyên mật khẩu cũ nếu truyền vào chuỗi rỗng.
    @Test
    void testSetPasswordBlank() {
        // Bước 1: Thử gán mật khẩu bằng một chuỗi rỗng
        seller.setPassword("");
        // Bước 2: Kiểm tra xem mật khẩu gốc có được giữ nguyên không
        assertEquals("123456", seller.getPassword());
    }

    // test 16: Chống lỗi sập hệ thống bằng cách chặn cập nhật mật khẩu với giá trị null.
    @Test
    void testSetPasswordNull() {
        // Bước 1 & 2: Truyền giá trị null và kiểm tra hệ thống ném ra IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> seller.setPassword(null),
                "Phai nem ra ngoai le khi password bị truyen vao la null");
    }

    // Test 17: Cập nhật thông tin họ và tên đầy đủ của người bán.
    @Test
    void testSetFullName() {
        // Bước 1: Cập nhật tên mới
        seller.setFullName("Nguyen Van A");
        // Bước 2: Xác nhận tên mới đã lưu
        assertEquals("Nguyen Van A", seller.getFullName());
    }

    // Test 18: Cập nhật thông tin số điện thoại.
    @Test
    void testSetPhoneNumber() {
        // Bước 1: Đổi số điện thoại liên lạc
        seller.setPhoneNumber("0123456789");
        // Bước 2: Kiểm tra số điện thoại mới được cập nhật chuẩn xác
        assertEquals("0123456789", seller.getPhoneNumber());
    }

    // test 19: Thay đổi trạng thái hoạt động (khóa/mở tài khoản) của Seller.
    @Test
    void testSetActive() {
        // Bước 1: Chuyển trạng thái hoạt động sang false (vô hiệu hóa)
        seller.setActive(false);
        // Bước 2: Xác nhận trạng thái lưu trên đối tượng
        assertFalse(seller.isActive());
    }

    // Test 20: Đảm bảo hệ thống trả về đúng tên vai trò phân quyền là "Seller".
    @Test
    void testRoleName() {
        // Bước 1 & 2: Kiểm tra chuỗi tên vai trò trả về
        assertEquals("Seller", seller.getRoleName());
    }

    // Test 21: Kiểm tra hàm displayRole() in đúng thông tin vai trò ra màn hình console.
    @Test
    void testDisplayRole() {
        // Bước 1: Bắt luồng xuất dữ liệu của console vào bộ nhớ đệm
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream original = System.out;

        // Chuyển hướng đầu ra console sang buffer bộ nhớ để kiểm tra nội dung in
        System.setOut(new PrintStream(output));

        // Bước 2: Kích hoạt hàm cần test
        seller.displayRole();

        // Trả console về trạng thái mặc định của hệ thống
        System.setOut(original);

        // Bước 3: Xác nhận thông tin in ra chứa dữ liệu mong đợi
        assertTrue(output.toString().contains("Role: Seller seller123"));
    }

    // Test 22: Xác nhận Seller là một lớp con kế thừa từ lớp cha User.
    @Test
    void testIsUser() {
        // Bước 1 & 2: Sử dụng instanceof để kiểm tra tính kế thừa
        assertTrue(seller instanceof User);
    }

    // test 23: Đảm bảo Seller có triển khai (implement) giao thức WalletOwner để thao tác với ví.
    @Test
    void testImplementsWalletOwner() {
        // Bước 1 & 2: Kiểm tra class Seller có gắn với interface WalletOwner hay không
        assertTrue(seller instanceof WalletOwner);
    }

    // Test 24: Xác nhận đối tượng Seller hỗ trợ Serialize để truyền/lưu dữ liệu nhị phân.
    @Test
    void testSerializable() {
        // Bước 1 & 2: Kiểm tra tính năng hỗ trợ giao diện Serializable
        assertTrue(seller instanceof Serializable);
    }

    // Test 25: Đảm bảo khi lưu đối tượng Seller xuống file nhị phân rồi đọc lên, dữ liệu hoàn toàn nguyên vẹn.
    @Test
    void testSerialization() throws IOException, ClassNotFoundException {
        // Bước 1: Mở luồng nhị phân và ghi toàn bộ dữ liệu của đối tượng Seller xuống bộ nhớ
        // Tiến hành ghi đối tượng ra luồng Byte độc lập
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bos);
        out.writeObject(seller);
        out.close();

        // Bước 2: Đọc ngược lại dữ liệu nhị phân vừa ghi để tạo ra một bản sao mới
        // Tiến hành đọc ngược lại luồng Byte thành đối tượng mới
        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream in = new ObjectInputStream(bis);
        Seller copy = (Seller) in.readObject();

        // Bước 3: So sánh từng thuộc tính lõi để chứng minh quá trình đọc/ghi thành công
        // Kiểm tra tính đồng nhất của dữ liệu trước và sau khi mã hóa nhị phân
        assertEquals(seller.getId(), copy.getId());
        assertEquals(seller.getUsername(), copy.getUsername());
        assertEquals(seller.getPassword(), copy.getPassword());
        assertEquals(seller.getPhoneNumber(), copy.getPhoneNumber());
    }
}