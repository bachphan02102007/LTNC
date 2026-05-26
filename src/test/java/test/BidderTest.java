package test;

import model.Bidder;
import model.User;
import model.WalletOwner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;

import static org.junit.jupiter.api.Assertions.*;

class BidderTest {

    private Bidder bidder;

    // Chạy trước mỗi test case để đảm bảo luôn có một đối tượng bidder chuẩn, mới hoàn toàn
    @BeforeEach
    void setUp() {
        bidder = new Bidder(
                "B001",
                "duy123",
                "123456",
                "0987654321"
        );
    }

    // Test 1: Đảm bảo khi tạo mới Bidder thiếu số điện thoại, hệ thống vẫn gán giá trị mặc định chuẩn.
    @Test
    void testConstructorWithoutPhoneNumber() {
        // Bước 1: Khởi tạo một đối tượng Bidder mới mà không truyền tham số số điện thoại
        Bidder b = new Bidder("B002", "testUser", "password");
        // Bước 2: Xác nhận hệ thống tự động gán các thông tin cơ bản chính xác
        assertEquals("B002", b.getId());
        assertEquals("testUser", b.getUsername());
        assertEquals("password", b.getPassword());
        // Bước 3: Kiểm tra các giá trị mặc định (số điện thoại là chuỗi rỗng, ví bằng 0, trạng thái active)
        assertEquals("", b.getPhoneNumber()); // Mặc định là chuỗi rỗng
        assertEquals(0, b.getWalletBalance());
        assertTrue(b.isActive());
    }

    // Test 2: Kiểm tra xem constructor có lưu đầy đủ thông tin khi được truyền đủ tham số hay không.
    @Test
    void testConstructorWithPhoneNumber() {
        // Bước 1 & 2: Đối tượng 'bidder' đã được tạo sẵn ở hàm setUp() với đầy đủ thông tin

        // Bước 3: Kiểm tra xem toàn bộ dữ liệu truyền vào lúc khởi tạo có được lưu trữ đúng không
        assertEquals("B001", bidder.getId());
        assertEquals("duy123", bidder.getUsername());
        assertEquals("123456", bidder.getPassword());
        assertEquals("0987654321", bidder.getPhoneNumber());
        assertEquals("duy123", bidder.getFullName());
        assertEquals(0, bidder.getWalletBalance());
        assertTrue(bidder.isActive());
    }

    // Test 3: Đảm bảo người dùng (Bidder) được hệ thống cấp quyền sử dụng ví tiền.
    @Test
    void testSupportsWallet() {
        // Bước 1: Gọi hàm kiểm tra tính năng hỗ trợ ví tiền của đối tượng
        // Bước 2: Kỳ vọng đối tượng Bidder bắt buộc phải hỗ trợ quản lý ví tiền (trả về true)
        assertTrue(bidder.supportsWallet());
    }

    // Test 4: Kiểm tra việc cập nhật số dư ví với một số tiền hợp lệ.
    @Test
    void testSetWalletBalance() {
        // Bước 1: Tiến hành gán một mức số dư ví hợp lệ (500) cho người dùng
        bidder.setWalletBalance(500);
        // Bước 2: Xác nhận hệ thống đã lưu trữ chính xác mốc số dư mới này
        assertEquals(500, bidder.getWalletBalance());
    }

    // Test 5: Ngăn chặn lỗi gán số dư ví bằng một con số âm.
    @Test
    void testSetWalletBalanceNegative() {
        // Bước 1: Cố tình thiết lập số dư ví bằng một con số âm (-100)
        // Bước 2: Kỳ vọng hệ thống chặn lại và ném ra lỗi IllegalArgumentException
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> bidder.setWalletBalance(-100));
        // Bước 3: Xác nhận thông điệp cảnh báo lỗi hiển thị chính xác
        assertEquals("So du vi khong duoc am", exception.getMessage());
    }

    // Test 6: Kiểm tra nạp tiền hợp lệ vào ví, số dư phải tăng tương ứng.
    @Test
    void testDeposit() {
        // Bước 1: Thực hiện hành động nạp một khoản tiền hợp lệ (300) vào ví
        bidder.deposit(300);
        // Bước 2: Kiểm tra xem số dư tổng trong ví có tăng lên đúng mốc 300 hay chưa
        assertEquals(300, bidder.getWalletBalance());
    }

    // Test 7: Chặn hành vi nạp tiền với giá trị âm.
    @Test
    void testDepositNegative() {
        // Bước 1: Cố tình thực hiện lệnh nạp một khoản tiền âm (-50) vào hệ thống
        // Bước 2: Xác nhận hệ thống sẽ ném ra ngoại lệ IllegalArgumentException để chặn hành vi này
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> bidder.deposit(-50));
        // Bước 3: Kiểm tra nội dung chuỗi tin nhắn báo lỗi của hệ thống
        assertEquals("So tien nap phai lon hon 0", exception.getMessage());
    }

    // Test 8: Chặn hành vi nạp tiền với giá trị bằng 0.
    @Test
    void testDepositZero() {
        // Bước 1: Thực hiện nạp tiền tại mốc biên bằng đúng giá trị 0
        // Bước 2: Kỳ vọng hệ thống từ chối giao dịch và báo lỗi đầu vào không hợp lệ
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> bidder.deposit(0));
        assertEquals("So tien nap phai lon hon 0", exception.getMessage());
    }

    // Test 9: Kiểm tra rút một phần tiền hợp lệ, số dư phải giảm tương ứng.
    @Test
    void testWithdraw() {
        // Bước 1: Nạp trước vào ví một lượng tiền là 500 để làm cơ sở dữ liệu
        bidder.deposit(500);
        // Bước 2: Tiến hành thực hiện lệnh rút một phần tiền (200) ra khỏi ví
        bidder.withdraw(200);
        // Bước 3: Xác nhận số dư còn lại trong ví được trừ chính xác (500 - 200 = 300)
        assertEquals(300, bidder.getWalletBalance());
    }

    // Test 10: Đảm bảo người dùng có thể rút sạch toàn bộ số tiền đang có trong ví.
    @Test
    void testWithdrawAll() {
        // Bước 1: Nạp sẵn vào tài khoản người dùng khoản tiền 500
        bidder.deposit(500);
        // Bước 2: Gọi lệnh rút sạch toàn bộ 500 đang có trong tài khoản
        bidder.withdraw(500);
        // Bước 3: Đảm bảo tài khoản xử lý trơn tru và số dư ví về đúng mốc 0
        assertEquals(0, bidder.getWalletBalance());
    }

    // Test 11: Chặn hành vi rút tiền với giá trị âm.
    @Test
    void testWithdrawNegative() {
        // Bước 1: Cố tình thực hiện hành động rút một khoản tiền mang giá trị âm (-10)
        // Bước 2: Kỳ vọng hệ thống ném ra lỗi ngoại lệ chặn thao tác sai trái này
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> bidder.withdraw(-10));
        assertEquals("So tien rut phai lon hon 0", exception.getMessage());
    }

    // Test 12: Chặn hành vi rút tiền với giá trị bằng 0.
    @Test
    void testWithdrawZero() {
        // Bước 1: Thực hiện thao tác rút tiền với giá trị bằng đúng mốc số 0
        // Bước 2: Đảm bảo logic xử lý từ chối thao tác và ném lỗi yêu cầu số tiền phải lớn hơn 0
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> bidder.withdraw(0));
        assertEquals("So tien rut phai lon hon 0", exception.getMessage());
    }

    // Test 13: Ngăn chặn rút số tiền vượt quá số dư hiện tại của ví.
    @Test
    void testWithdrawInsufficientBalance() {
        // Bước 1: Thiết lập số tiền hiện có trong ví ở mức thấp (100)
        bidder.deposit(100);
        // Bước 2: Cố tình rút một lượng tiền vượt mức cho phép (200)
        // Bước 3: Kiểm tra hệ thống ném ra đúng lỗi thông báo ví không đủ tiền thanh toán
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> bidder.withdraw(200));
        assertEquals("So du vi khong du", exception.getMessage());
    }

    // Test 14: Cập nhật mật khẩu hợp lệ mới cho tài khoản.
    @Test
    void testSetPassword() {
        // Bước 1: Tiến hành cập nhật mật khẩu mới cho người dùng
        bidder.setPassword("newpass");
        // Bước 2: Xác nhận hệ thống đã lưu đè chuỗi mật khẩu mới thành công
        assertEquals("newpass", bidder.getPassword());
    }

    // Test 15: Bỏ qua và giữ nguyên mật khẩu cũ nếu người dùng truyền vào chuỗi rỗng.
    @Test
    void testSetPasswordBlank() {
        // Bước 1: Thử thiết lập mật khẩu bằng một chuỗi ký tự trống rỗng
        bidder.setPassword("");
        // Bước 2: Hệ thống phải bỏ qua chuỗi rỗng này và giữ nguyên vẹn mật khẩu ban đầu là "123456"
        assertEquals("123456", bidder.getPassword());
    }

    // Test 16: Bắt lỗi và chặn việc cập nhật mật khẩu mang giá trị null.
    @Test
    void testSetPasswordNull() {
        // Bước 1: Cố tình truyền giá trị tham chiếu 'null' vào hàm thay đổi mật khẩu
        // Bước 2: Đảm bảo hệ thống chủ động chặn đứng lỗi tiềm ẩn và ném ra ngoại lệ phù hợp
        assertThrows(IllegalArgumentException.class, () -> bidder.setPassword(null),
                "Phai nem ra ngoai le khi password null");
    }

    // Test 17: Cập nhật tên đầy đủ của người dùng.
    @Test
    void testSetFullName() {
        // Bước 1: Thực hiện thiết lập chuỗi họ tên đầy đủ mới cho người dùng
        bidder.setFullName("Nguyen Van Duy");
        // Bước 2: Xác nhận dữ liệu họ tên đã cập nhật thành công trên đối tượng
        assertEquals("Nguyen Van Duy", bidder.getFullName());
    }

    // Test 18: Cập nhật số điện thoại liên lạc của người dùng.
    @Test
    void testSetPhoneNumber() {
        // Bước 1: Tiến hành thay đổi thông tin số điện thoại liên lạc
        bidder.setPhoneNumber("0123456789");
        // Bước 2: Đảm bảo số điện thoại mới được cập nhật chính xác tuyệt đối
        assertEquals("0123456789", bidder.getPhoneNumber());
    }

    // Test 19: Thay đổi trạng thái hoạt động (kích hoạt/vô hiệu hóa) của tài khoản.
    @Test
    void testSetActive() {
        // Bước 1: Chuyển trạng thái hoạt động của người dùng về mức vô hiệu hóa (false)
        bidder.setActive(false);
        // Bước 2: Xác nhận trạng thái tài khoản hiện tại đã đổi thành ngưng hoạt động thành công
        assertFalse(bidder.isActive());
    }

    // Test 20: Đảm bảo hai đối tượng Bidder có cùng ID được hệ thống nhận diện là một.
    @Test
    void testEqualsAndHashCode() {
        // Bước 1: Khởi tạo 2 đối tượng mới: 1 đối tượng trùng ID "B001" với thực thể gốc, 1 đối tượng mang ID "B009" hoàn toàn khác
        Bidder sameBidder = new Bidder("B001", "otherUser", "pass", "1111");
        Bidder differentBidder = new Bidder("B009", "duy123", "123456", "0987");

        // Bước 2: Kiểm tra logic equals - Hai đối tượng trùng ID bắt buộc phải coi là bằng nhau về mặt định danh
        assertEquals(bidder, sameBidder, "Hai bidder co cung ID phai bang nhau");
        assertNotEquals(bidder, differentBidder, "Hai bidder khac ID phai khac nhau");
        // Bước 3: Đảm bảo tính đồng bộ của hàm băm - Nếu hai đối tượng bằng nhau thì mã hashCode của chúng phải trùng khớp
        assertEquals(bidder.hashCode(), sameBidder.hashCode());
    }

    // test 22: Xác nhận tên vai trò phân quyền của đối tượng luôn là "Bidder".
    @Test
    void testRoleName() {
        // Bước 1 & 2: Lấy chuỗi mô tả tên vai trò hệ thống của đối tượng và đảm bảo nó khớp chính xác với từ khóa "Bidder"
        assertEquals("Bidder", bidder.getRoleName());
    }

    // Test 23: Đảm bảo Bidder kế thừa đúng từ lớp cha User.
    @Test
    void testIsUser() {
        // Bước 1 & 2: Dùng từ khóa instanceof để xác nhận lớp Bidder kế thừa chuẩn xác từ lớp cha trừu tượng User
        assertTrue(bidder instanceof User);
    }

    // Test 24: Đảm bảo Bidder tuân thủ giao thức WalletOwner để thao tác với ví.
    @Test
    void testImplementsWalletOwner() {
        // Bước 1 & 2: Đảm bảo cấu trúc class Bidder có hiện thực hóa (implement) bản thiết kế WalletOwner để xử lý các nghiệp vụ ví tiền
        assertTrue(bidder instanceof WalletOwner);
    }

    // Test 25: Kiểm tra xem đối tượng có thể được mã hóa để lưu trữ hoặc truyền tải qua mạng không.
    @Test
    void testSerializable() {
        // Bước 1 & 2: Xác nhận lớp có khả năng tuần tự hóa dữ liệu để phục vụ việc lưu file hoặc truyền tải luồng mạng sau này
        assertTrue(bidder instanceof Serializable);
    }

    // Test 26: Kiểm tra luồng ghi và đọc dữ liệu nhị phân của đối tượng, đảm bảo không bị mất mát dữ liệu.
    @Test
    void testSerialization() throws IOException, ClassNotFoundException {
        // Ghi đối tượng vào luồng bộ nhớ
        // Bước 1: Mở luồng bộ nhớ nhị phân và tiến hành ghi (tuần tự hóa) toàn bộ dữ liệu của đối tượng 'bidder' hiện tại xuống
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bos);
        out.writeObject(bidder);
        out.close();

        // Đọc đối tượng ra từ luồng bộ nhớ
        // Bước 2: Tiến hành đọc ngược lại luồng nhị phân vừa ghi để tạo ra một thực thể bản sao 'copy' độc lập hoàn toàn trong bộ nhớ
        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream in = new ObjectInputStream(bis);
        Bidder copy = (Bidder) in.readObject();

        // So sánh từng thuộc tính
        // Bước 3: So sánh chi tiết từng thuộc tính lõi giữa đối tượng gốc ban đầu và đối tượng vừa hồi phục để chứng minh tính toàn vẹn dữ liệu
        assertEquals(bidder.getId(), copy.getId());
        assertEquals(bidder.getUsername(), copy.getUsername());
        assertEquals(bidder.getPassword(), copy.getPassword());
        assertEquals(bidder.getPhoneNumber(), copy.getPhoneNumber());
    }
}