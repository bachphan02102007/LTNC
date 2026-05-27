package test;

import exception.InvalidBidException;
import model.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ServerBroadcastObserverTest {

    static class TestBroadcastObserver implements AuctionObserver {
        String lastMessage = null;

        @Override
        public void onBidUpdated(Auction auction, BidTransaction tx) {
            lastMessage = "GIA_MOI:" + auction.getAuctionId() + ":"
                    + tx.getAmount() + ":" + tx.getBidder().getUsername();
        }

        @Override
        public void onAuctionClosed(Auction auction) {
        }
    }

    static class TestItem extends Item {
        public TestItem(String id, String name, String description, double startingPrice) {
            super(id, name, description, startingPrice);
        }

        @Override
        public String printInfo() {
            return "Test Item";
        }
    }

    // Test 1: Kiểm tra khi đặt giá hợp lệ, hệ thống phải cập nhật đúng giá mới
    // và gửi đi thông báo chính xác cho mọi người.
    @Test
    void testBroadcastOnSuccessfulBid() throws Exception {
        // Bước 1: Chuẩn bị một phiên đấu giá và gắn bộ lắng nghe (observer) vào để theo dõi
        Item item = new TestItem("ITEM1", "Laptop", "Gaming", 100);
        Auction auction = new Auction("A001", item, LocalDateTime.now().plusMinutes(1));
        auction.startAuction();

        TestBroadcastObserver observer = new TestBroadcastObserver();
        auction.addObserver(observer);
        Bidder bidder = new Bidder("B1", "duy", "123");
        bidder.setWalletBalance(1000.0);

        // Bước 2: Người dùng thực hiện đặt một mức giá hợp lệ (500 lớn hơn giá gốc 100)
        auction.placeBid(bidder, 500.0);

        // Bước 3: Kiểm tra kết quả
        // - Bộ lắng nghe phải nhận được đúng tin nhắn với định dạng chuẩn
        assertEquals("GIA_MOI:A001:500.0:duy", observer.lastMessage);
        // - Hệ thống phải ghi nhận giá cao nhất hiện tại là 500
        assertEquals(500.0, auction.getCurrentHighestBid());
        // - Người dẫn đầu phiên đấu giá lúc này phải là "duy"
        assertEquals("duy", auction.getCurrentLeader().getUsername());
    }

    // Test 2: Kiểm tra khi đặt giá không hợp lệ (thấp hơn giá hiện tại),
    // hệ thống phải chặn lại báo lỗi, giữ nguyên dữ liệu cũ và không gửi thông báo rác.
    @Test
    void testBroadcastNotCalledWhenBidFails() throws Exception {
        // Bước 1: Chuẩn bị phiên đấu giá và đặt trước một mức giá khởi điểm thành công là 500
        Item item = new TestItem("ITEM2", "Phone", "Iphone", 100);
        Auction auction = new Auction("A002", item, LocalDateTime.now().plusMinutes(1));
        auction.startAuction();

        TestBroadcastObserver observer = new TestBroadcastObserver();
        auction.addObserver(observer);
        Bidder bidder = new Bidder("B1", "duy", "123");
        bidder.setWalletBalance(1000.0);
        auction.placeBid(bidder, 500);

        // Bước 2: Cố tình đặt một mức giá thấp hơn (300) để tạo lỗi
        assertThrows(
                InvalidBidException.class,
                () -> auction.placeBid(bidder, 300)
        );

        // Bước 3: Kiểm tra xem hệ thống có bảo vệ dữ liệu cũ thành công không
        // - Giá cao nhất không bị tụt xuống 300 mà vẫn giữ nguyên 500
        assertEquals(500.0, auction.getCurrentHighestBid());
        // - Người dẫn đầu vẫn không bị thay đổi
        assertEquals("duy", auction.getCurrentLeader().getUsername());
        // - Tin nhắn cuối cùng mà hệ thống phát ra vẫn là của lượt 500 (không phát thêm tin nhắn rác cho lượt 300)
        assertEquals("GIA_MOI:A002:500.0:duy", observer.lastMessage);
    }
}