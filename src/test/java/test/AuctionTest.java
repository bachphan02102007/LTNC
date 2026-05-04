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
    }

    //Test 1: Đặt giá hợp lệ
    @Test
    void testPlaceBid_ValidAmount_ShouldSucceed() throws Exception {
        auction.placeBid(alice, 600.0);
        assertEquals(600.0, auction.getCurrentHighestBid());
        assertEquals(alice, auction.getCurrentLeader());
    }

    //Test 2: Đặt giá thấp hơn giá hiện tại → phải throw InvalidBidException
    @Test
    void testPlaceBid_LowerAmount_ShouldThrowInvalidBidException() throws Exception {
        auction.placeBid(alice, 600.0);
        assertThrows(InvalidBidException.class, () -> {
            auction.placeBid(bob, 550.0); // thấp hơn 600 → lỗi
        });
    }

    //Test 3: Đặt giá bằng giá hiện tại → cũng phải lỗi
    @Test
    void testPlaceBid_EqualAmount_ShouldThrowInvalidBidException() throws Exception {
        auction.placeBid(alice, 600.0);
        assertThrows(InvalidBidException.class, () -> {
            auction.placeBid(bob, 600.0); // bằng → lỗi
        });
    }

    //Test 4: Đặt giá sau khi phiên đóng → phải throw AuctionClosedException
    @Test
    void testPlaceBid_AfterClose_ShouldThrowAuctionClosedException() throws Exception {
        auction.placeBid(alice, 600.0);
        auction.closeAuction();
        assertThrows(AuctionClosedException.class, () -> {
            auction.placeBid(bob, 700.0); // phiên đã đóng → lỗi
        });
    }

    //Test 5: Kết thúc phiên có người thắng → status FINISHED
    @Test
    void testCloseAuction_WithBids_ShouldBeFinished() throws Exception {
        auction.placeBid(alice, 600.0);
        auction.closeAuction();
        assertEquals(AuctionStatus.FINISHED, auction.getStatus());
        assertEquals(alice, auction.getCurrentLeader());
    }

    // Test 6: Kết thúc phiên không có ai đặt → status CANCELED
    @Test
    void testCloseAuction_NoBids_ShouldBeCanceled() {
        auction.closeAuction();
        assertEquals(AuctionStatus.CANCELED, auction.getStatus());
        assertNull(auction.getCurrentLeader());
    }

    //Test 7: Lịch sử đặt giá được lưu đúng
    @Test
    void testBidHistory_ShouldRecordAllValidBids() throws Exception {
        auction.placeBid(alice, 600.0);
        auction.placeBid(bob,   700.0);
        auction.placeBid(alice, 800.0);
        assertEquals(3, auction.getBidHistory().size());
    }

    //Test 8: Test concurrency — 2 thread đặt giá cùng lúc, không được lost update
    @Test
    void testConcurrentBidding_ShouldNotCauseLostUpdate() throws InterruptedException {
        int[] successCount = {0};
        Thread t1 = new Thread(() -> {
            try { auction.placeBid(alice, 600.0); successCount[0]++; }
            catch (Exception ignored) {}
        });
        Thread t2 = new Thread(() -> {
            try { auction.placeBid(bob, 600.0); successCount[0]++; }
            catch (Exception ignored) {}
        });
        t1.start(); t2.start();
        t1.join();  t2.join();

        // Chỉ đúng 1 trong 2 thread thắng — không được cả 2 cùng thắng
        assertEquals(1, successCount[0]);
        assertEquals(600.0, auction.getCurrentHighestBid());
    }
}