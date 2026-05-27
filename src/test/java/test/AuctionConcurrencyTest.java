package test;

import exception.AuctionClosedException;
import exception.InvalidBidException;
import model.Auction;
import model.Bidder;
import model.Item;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class AuctionConcurrencyTest {

    static class TestItem extends Item {
        public TestItem(String id, String name, String description, double startingPrice) {
            super(id, name, description, startingPrice);
        }

        @Override
        public String printInfo() {
            return "Test Item";
        }
    }

    // Test 1: Kiểm tra khi nhiều người chơi ập vào đặt các mức giá khác nhau CÙNG MỘT LÚC,
    // hệ thống có bị mất dữ liệu và vẫn chốt được người trả giá cao nhất không.
    @Test
    void testConcurrentBidding() throws InterruptedException {
        // Bước 1: Khởi tạo dữ liệu
        Item item = new TestItem("ITEM1", "Laptop", "Gaming", 100);
        Auction auction = new Auction("A001", item, LocalDateTime.now().plusMinutes(1));
        auction.startAuction();

        int numberOfBidders = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfBidders);
        CountDownLatch ready = new CountDownLatch(numberOfBidders); // Chờ 10 thread sẵn sàng
        CountDownLatch start = new CountDownLatch(1);               // Súng lệnh bắt đầu

        double[] bids = {120, 150, 130, 200, 180, 170, 220, 210, 250, 240};

        // Bước 2: Tạo 10 luồng (thread) ôm sẵn mức giá, vào vạch xuất phát
        for (int i = 0; i < numberOfBidders; i++) {
            Bidder bidder = new Bidder("B" + i, "bidder" + i, "123");
            final int index = i;

            executor.submit(() -> {
                try {
                    ready.countDown(); // Báo cáo: "Tôi đã sẵn sàng"
                    start.await();     // Nín thở chờ súng lệnh
                    auction.placeBid(bidder, bids[index]);
                } catch (Exception ignored) {
                    // Chấp nhận việc các bid thấp (chạy sau) bị hệ thống ném exception từ chối
                }
            });
        }

        // Bước 3: Bắn súng lệnh kích hoạt 10 thread bắn request vào Auction cùng 1 mili-giây
        ready.await();
        start.countDown();

        // Bước dọn dẹp: Đợi các thread chạy xong
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // Bước 4: Kiểm tra kết quả
        assertEquals(250, auction.getCurrentHighestBid(), "Lỗi: Race Condition làm sai lệch giá cao nhất!");
        assertEquals("bidder8", auction.getCurrentLeader().getUsername());
    }

    // Test 2: Kiểm tra kịch bản 5 người đặt giá y hệt nhau cùng lúc.
    // Hệ thống chỉ được ghi nhận 1 người nhanh nhất.
    @Test
    void testSameAmountConcurrentBid() throws InterruptedException {
        Item item = new TestItem("ITEM2", "Phone", "Iphone", 100);
        Auction auction = new Auction("A002", item, LocalDateTime.now().plusMinutes(1));
        auction.startAuction();

        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);

        for (int i = 0; i < threadCount; i++) {
            int index = i;
            executor.submit(() -> {
                try {
                    ready.countDown();
                    start.await();
                    // 5 người đều tranh nhau đặt đúng 500
                    auction.placeBid(new Bidder("B" + index, "bidder" + index, "123"), 500);
                } catch (Exception ignored) {}
            });
        }

        ready.await();
        start.countDown();
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // Đảm bảo chỉ duy nhất 1 giao dịch được tạo ra
        assertEquals(500, auction.getCurrentHighestBid());
        assertEquals(1, auction.getBidHistory().size(), "Lỗi: Hệ thống lưu trùng 2 lượt bid có cùng mức giá!");
    }

    // Test 3: Stress Test ép hệ thống chịu tải 1000 request tăng dần liên tục.
    @Test
    void testHighConcurrencyStress() throws InterruptedException {
        Item item = new TestItem("ITEM3", "PC", "Gaming PC", 100);
        Auction auction = new Auction("A003", item, LocalDateTime.now().plusMinutes(1));
        auction.startAuction();

        int requests = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(requests);

        for (int i = 0; i < requests; i++) {
            int price = i + 101; // Giá cao nhất sẽ là 1100
            executor.submit(() -> {
                try {
                    auction.placeBid(new Bidder("B", "bidder", "123"), price);
                } catch (Exception ignored) {
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        // Đảm bảo giá đỉnh (1100) không bị ghi đè bởi các thread yếu (giá thấp nhưng chạy chậm)
        assertEquals(1100, auction.getCurrentHighestBid(), "Lỗi: Race Condition làm mất giá đỉnh dưới tải nặng!");
    }

    // Test 4: Kiểm tra độ ổn định khi Admin đóng phiên ĐÚNG LÚC nhiều người đang ùa vào đặt giá
    @Test
    void testBidWhileAuctionClosingConcurrent() throws InterruptedException {
        Item item = new TestItem("ITEM4", "Watch", "Apple Watch", 100);
        Auction auction = new Auction("A004", item, LocalDateTime.now().plusMinutes(1));
        auction.startAuction();

        int bidderThreads = 20;
        ExecutorService executor = Executors.newFixedThreadPool(bidderThreads + 1);
        CountDownLatch ready = new CountDownLatch(bidderThreads + 1);
        CountDownLatch start = new CountDownLatch(1);

        // Biến đếm an toàn trong đa luồng
        AtomicInteger closedExceptionCount = new AtomicInteger(0);
        AtomicInteger invalidExceptionCount = new AtomicInteger(0);
        AtomicInteger successBidCount = new AtomicInteger(0);

        // 20 Bidder lăm le đặt giá
        for (int i = 0; i < bidderThreads; i++) {
            int price = 200 + i;
            executor.submit(() -> {
                try {
                    ready.countDown();
                    start.await();

                    // Nạp tiền để tránh lỗi IllegalArgumentException do ví rỗng
                    Bidder bidder = new Bidder("B", "bidder", "123");
                    bidder.setWalletBalance(1000000.0);

                    auction.placeBid(bidder, price);
                    successBidCount.incrementAndGet();
                } catch (AuctionClosedException e) {
                    closedExceptionCount.incrementAndGet(); // Bị chặn do phiên đã đóng
                } catch (InvalidBidException e) {
                    invalidExceptionCount.incrementAndGet(); // Bị chặn do giá thấp hơn
                } catch (Exception ignored) {
                }
            });
        }

        // 1 Thread Admin lăm le đóng phiên
        executor.submit(() -> {
            try {
                ready.countDown();
                start.await();
                auction.closeAuction();
            } catch (Exception ignored) {}
        });

        // Kích hoạt tất cả các luồng chạy cùng lúc
        ready.await();
        start.countDown();

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // Tổng số luồng hợp lệ phải được bảo toàn, không được thất thoát
        assertEquals(
                bidderThreads,
                successBidCount.get() + closedExceptionCount.get() + invalidExceptionCount.get(),
                "Lỗi: Trạng thái luồng xử lý bị thất thoát khi xảy ra xung đột đóng phiên!"
        );
    }}