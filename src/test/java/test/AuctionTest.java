package test;

import exception.AuctionClosedException;
import exception.InvalidBidException;
import model.*;
import org.junit.jupiter.api.*;
import util.Factory.ItemFactory;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AuctionTest {

    private Auction auction;
    private Bidder alice;
    private Bidder bob;

    @BeforeEach
    void setUp() {
        // Chạy trước mỗi test — tạo dữ liệu sạch
        Item laptop = ItemFactory.create("ELECTRONICS", "I001",
                "Laptop Dell", "Core i7", 500.0, "12");
        auction = new Auction("A001", laptop,
                LocalDateTime.now().plusHours(1));
        auction.startAuction();
        alice = new Bidder("U001", "alice", "123");
        bob   = new Bidder("U002", "bob",   "456");
        alice.setWalletBalance(1000000.0);
        bob.setWalletBalance(1000000.0);
    }

    //Test 1: Đặt giá hợp lệ
    @Test
    void testPlaceBid_ValidAmount_ShouldSucceed() throws Exception {
        // Bước 1: Thực hiện đặt mức giá hợp lệ (600.0 lớn hơn giá khởi điểm 500.0)
        auction.placeBid(alice, 600.0);

        // Bước 2: Kiểm tra xem hệ thống đã cập nhật giá cao nhất lên 600.0 chưa
        assertEquals(600.0, auction.getCurrentHighestBid());

        // Bước 3: Xác nhận Alice đã trở thành người dẫn đầu phiên đấu giá hiện tại
        assertEquals(alice, auction.getCurrentLeader());
    }

    //Test 2: Đặt giá thấp hơn giá hiện tại → phải throw InvalidBidException
    @Test
    void testPlaceBid_LowerAmount_ShouldThrowInvalidBidException() throws Exception {
        // Bước 1: Alice đặt giá 600.0 thành công, nâng giá sàn hiện tại lên 600.0
        auction.placeBid(alice, 600.0);

        // Bước 2: Bob nhảy vào sau nhưng cố tình trả mức giá 550.0 (thấp hơn giá sàn)
        // Bước 3: Kỳ vọng hệ thống chặn lại và ném ra lỗi InvalidBidException
        assertThrows(InvalidBidException.class, () -> {
            auction.placeBid(bob, 550.0); // thấp hơn 600 → lỗi
        });
    }

    //Test 3: Đặt giá bằng giá hiện tại → cũng phải lỗi
    @Test
    void testPlaceBid_EqualAmount_ShouldThrowInvalidBidException() throws Exception {
        // Bước 1: Alice đặt giá 600.0 thành công để làm mốc dẫn đầu
        auction.placeBid(alice, 600.0);

        // Bước 2: Bob cố tình trả giá 600.0 bằng y hệt mức giá của Alice
        // Bước 3: Kỳ vọng hệ thống không chấp nhận giá trùng và ném ra lỗi InvalidBidException
        assertThrows(InvalidBidException.class, () -> {
            auction.placeBid(bob, 600.0); // bằng → lỗi
        });
    }

    //Test 4: Đặt giá sau khi phiên đóng → phải throw AuctionClosedException
    @Test
    void testPlaceBid_AfterClose_ShouldThrowAuctionClosedException() throws Exception {
        // Bước 1: Alice đặt giá 600.0 khi phiên còn đang mở
        auction.placeBid(alice, 600.0);

        // Bước 2: Thực hiện hành động đóng phiên đấu giá lại
        auction.closeAuction();

        // Bước 3: Bob cố tình đặt giá 700.0 khi phiên đấu giá đã kết thúc
        // Kỳ vọng: Hệ thống phải từ chối và ném ra lỗi AuctionClosedException
        assertThrows(AuctionClosedException.class, () -> {
            auction.placeBid(bob, 700.0); // phiên đã đóng → lỗi
        });
    }

    //Test 5: Kết thúc phiên có người thắng → status FINISHED
    @Test
    void testCloseAuction_WithBids_ShouldBeFinished() throws Exception {
        // Bước 1: Alice tham gia và đặt mức giá 600.0 thành công
        auction.placeBid(alice, 600.0);

        // Bước 2: Thực hiện đóng phiên đấu giá
        auction.closeAuction();

        // Bước 3: Kiểm tra trạng thái phiên đấu giá bắt buộc phải chuyển sang FINISHED (Hoàn thành)
        assertEquals(AuctionStatus.FINISHED, auction.getStatus());

        // Bước 4: Đảm bảo Alice được chốt danh sách là người chiến thắng cuối cùng
        assertEquals(alice, auction.getCurrentLeader());
    }

    // Test 6: Kết thúc phiên không có ai đặt → status CANCELED
    @Test
    void testCloseAuction_NoBids_ShouldBeCanceled() {
        // Bước 1: Thực hiện đóng phiên đấu giá ngay lập tức khi chưa có bất kỳ ai đặt giá
        auction.closeAuction();

        // Bước 2: Kiểm tra trạng thái phiên phải chuyển thành CANCELED (Bị hủy vì không có người mua)
        assertEquals(AuctionStatus.CANCELED, auction.getStatus());

        // Bước 3: Đảm bảo hệ thống ghi nhận không có ai dẫn đầu hay thắng cuộc (trả về null)
        assertNull(auction.getCurrentLeader());
    }

    //Test 7: Lịch sử đặt giá được lưu đúng
    @Test
    void testBidHistory_ShouldRecordAllValidBids() throws Exception {
        // Bước 1: Thực hiện chuỗi 3 lượt đặt giá hợp lệ liên tiếp tăng dần từ cả Alice và Bob
        auction.placeBid(alice, 600.0);
        auction.placeBid(bob,   700.0);
        auction.placeBid(alice, 800.0);

        // Bước 2: Kiểm tra xem bộ nhớ lịch sử hệ thống có lưu trữ đầy đủ và chính xác cả 3 lượt này không
        assertEquals(3, auction.getBidHistory().size());
    }

    //Test 8: Test concurrency — 2 thread đặt giá cùng lúc, không được lost update
    @Test
    void testConcurrentBidding_ShouldNotCauseLostUpdate() throws InterruptedException {
        // Mảng chứa số lượng lượt đặt giá thành công (sử dụng mảng 1 phần tử để sửa đổi được trong luồng)
        int[] successCount = {0};

        // Bước 1: Khởi tạo luồng t1 - Alice cố gắng nhảy vào đặt giá 600.0
        Thread t1 = new Thread(() -> {
            try { auction.placeBid(alice, 600.0); successCount[0]++; }
            catch (Exception ignored) {}
        });

        // Bước 2: Khởi tạo luồng t2 - Bob cũng cố gắng đặt giá trùng 600.0 vào cùng thời điểm
        Thread t2 = new Thread(() -> {
            try { auction.placeBid(bob, 600.0); successCount[0]++; }
            catch (Exception ignored) {}
        });

        // Bước 3: Đồng thời kích hoạt cả 2 luồng chạy đua xử lý
        t1.start(); t2.start();

        // Bước 4: Đợi cả 2 luồng kết thúc tiến trình hoàn toàn
        t1.join();  t2.join();

        // Chỉ đúng 1 trong 2 thread thắng — không được cả 2 cùng thắng
        // Bước 5: Kiểm tra tính nhất quán, hệ thống chỉ được phép cho 1 người thành công duy nhất
        assertEquals(1, successCount[0]);

        // Bước 6: Đảm bảo giá cao nhất hiện tại được chốt an toàn ở mức 600.0 không bị lỗi ghi đè dữ liệu
        assertEquals(600.0, auction.getCurrentHighestBid());
    }
}