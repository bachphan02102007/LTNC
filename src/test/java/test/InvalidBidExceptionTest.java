package test;

import exception.InvalidBidException;
import model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class InvalidBidExceptionTest {

    private Auction auction;
    private Bidder alice;
    private Bidder bob;

    @BeforeEach
    void setUp() {
        // Tạo một sản phẩm thật (dùng class vô danh thay vì mock để test logic mượt hơn)
        Item item = new Item("I001", "Laptop", "Test Item", 100) {
            @Override
            public String printInfo() {
                return "Test Item";
            }
        };

        // Khởi tạo và mở một phiên đấu giá với giá khởi điểm của sản phẩm là 100
        auction = new Auction("A001", item, LocalDateTime.now().plusHours(1));
        auction.startAuction();

        // Tạo 2 người chơi và nạp sẵn tiền vào ví để test
        alice = new Bidder("U001", "alice", "123");
        bob   = new Bidder("U002", "bob", "456");

        alice.setWalletBalance(1000);
        bob.setWalletBalance(200);
    }

    // Test 1: Chặn người chơi đặt một mức giá bằng y hệt giá cao nhất hiện tại.
    @Test
    void testBidEqualCurrentPrice_ShouldThrowInvalidBidException() throws Exception {
        // Bước 1: Alice đặt giá 300 thành công
        auction.placeBid(alice, 300);

        // Bước 2: Bob cố tình đặt lại đúng mức 300 đó
        // Kỳ vọng: Hệ thống từ chối và ném ra lỗi InvalidBidException
        assertThrows(InvalidBidException.class, () -> {
            auction.placeBid(bob, 300);
        }, "Lỗi: Hệ thống không chặn việc đặt giá bằng với giá hiện tại!");
    }

    // Test 2: Chặn người chơi đặt một mức giá thấp hơn giá cao nhất hiện tại.
    @Test
    void testBidLowerThanCurrentPrice_ShouldThrowInvalidBidException() throws Exception {
        // Bước 1: Alice đặt giá dẫn đầu là 500
        auction.placeBid(alice, 500);

        // Bước 2: Bob nhảy vào sau nhưng chỉ đặt giá 400
        // Kỳ vọng: Hệ thống từ chối vì giá mới phải cao hơn 500
        assertThrows(InvalidBidException.class, () -> {
            auction.placeBid(bob, 400);
        }, "Lỗi: Hệ thống cho phép đặt giá thấp hơn người dẫn đầu!");
    }

    // Test 3: Chặn người chơi đặt giá khi số dư trong ví không đủ để thanh toán.
    @Test
    void testBidNotEnoughWallet_ShouldFail() {
        // Bước 1: Kiểm tra lại số dư của Bob (đang là 200)

        // Bước 2: Bob cố gắng đặt một mức giá là 300 (vượt quá tiền trong ví)
        // Kỳ vọng: Phải gọi qua hàm placeBid và hệ thống sẽ ném lỗi
        // (Lưu ý: Tùy logic code của bạn mà lỗi là IllegalArgumentException hay InvalidBidException)
        assertThrows(IllegalArgumentException.class, () -> {
            auction.placeBid(bob, 300);
        }, "Lỗi: Hệ thống không kiểm tra số dư ví khi đặt giá!");
    }

    // Test 4: Chặn ngay từ đầu nếu người chơi đặt giá thấp hơn cả giá khởi điểm của sản phẩm.
    @Test
    void testBidLowerThanStartingPrice_ShouldThrowInvalidBidException() {
        // Bước 1: Phiên đấu giá mới mở, chưa ai đặt, giá khởi điểm là 100

        // Bước 2: Alice vào mở bát nhưng lại đặt giá 50
        // Kỳ vọng: Hệ thống phải chặn ngay lập tức
        assertThrows(InvalidBidException.class, () -> {
            auction.placeBid(alice, 50);
        }, "Lỗi: Hệ thống cho phép đặt giá thấp hơn giá khởi điểm ban đầu!");
    }
}