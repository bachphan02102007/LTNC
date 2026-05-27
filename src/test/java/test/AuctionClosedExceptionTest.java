package test;

import exception.AuctionClosedException;
import model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AuctionClosedExceptionTest {

    private Auction auction;
    private Bidder alice;
    private Bidder bob;

    @BeforeEach
    void setUp() {
        // Tạo sản phẩm giả lập để phục vụ kiểm thử
        Item item = new Item("I001", "Laptop", "Test Item", 100) {
            @Override
            public String printInfo() {
                return "Test Item";
            }
        };

        // Khởi tạo một phiên đấu giá hợp lệ, thời gian kết thúc còn thoải mái (1 tiếng nữa)
        auction = new Auction("A001", item, LocalDateTime.now().plusHours(1));
        auction.startAuction();

        alice = new Bidder("U001", "alice", "123");
        bob   = new Bidder("U002", "bob", "456");

        alice.setWalletBalance(1000);
        bob.setWalletBalance(1000);
    }

    // Test 1: Chặn không cho đặt giá sau khi quản trị viên đã chủ động đóng phiên đấu giá bằng tay.
    @Test
    void testBidAfterManualClose_ShouldThrowAuctionClosedException() throws Exception {
        // Bước 1: Alice đặt một mức giá hợp lệ khi phiên đấu giá còn đang mở
        auction.placeBid(alice, 500);

        // Bước 2: Chủ sàn chủ động đóng phiên đấu giá lại
        auction.closeAuction();

        // Bước 3: Bob cố tình nhảy vào đặt giá sau khi phiên đã đóng
        // Kỳ vọng: Hệ thống phải chặn lại và ném ra lỗi AuctionClosedException
        assertThrows(AuctionClosedException.class, () -> {
            auction.placeBid(bob, 600);
        }, "Lỗi: Hệ thống vẫn cho đặt giá sau khi phiên đấu giá đã bị đóng thủ công!");
    }

    // Test 2: Hệ thống phải tự động chặn đặt giá khi phiên đấu giá đã quá giờ kết thúc (hết thời gian).
    @Test
    void testBidAfterTimeExpired_ShouldThrowAuctionClosedException() {
        // Bước 1: Tạo một phiên đấu giá đặc biệt có thời gian kết thúc từ 5 phút trước (đã hết giờ)
        Item item = new Item("I002", "Phone", "Expired Item", 100) {
            @Override
            public String printInfo() { return "Expired Item"; }
        };
        Auction expiredAuction = new Auction("A002", item, LocalDateTime.now().minusMinutes(5));
        expiredAuction.startAuction();

        // Bước 2: Người chơi cố tình đặt giá vào phiên đấu giá đã hết giờ này
        // Bước 3: Kỳ vọng hệ thống tự nhận biết và chặn lại, ném ra lỗi AuctionClosedException
        assertThrows(AuctionClosedException.class, () -> {
            expiredAuction.placeBid(alice, 200);
        }, "Lỗi: Hệ thống vẫn cho phép đặt giá khi phiên đấu giá đã hết thời gian chạy!");
    }

    // Test 3: Đảm bảo sau khi phiên đấu giá đã đóng, mọi lượt đặt giá liên tục sau đó đều phải bị từ chối.
    @Test
    void testMultipleBidsAfterClose_ShouldAlwaysFail() throws Exception {
        // Bước 1: Đặt giá hợp lệ và tiến hành đóng phiên đấu giá lại
        auction.placeBid(alice, 300);
        auction.closeAuction();

        // Bước 2: Liên tục thực hiện nhiều lượt đặt giá tiếp theo từ các người chơi khác nhau
        // Bước 3: Xác nhận tất cả các lượt đặt giá muộn này đều bị hệ thống chặn đứng thành công
        assertThrows(AuctionClosedException.class, () -> auction.placeBid(bob, 400));
        assertThrows(AuctionClosedException.class, () -> auction.placeBid(alice, 500));
        assertThrows(AuctionClosedException.class, () -> auction.placeBid(bob, 600));
    }
}