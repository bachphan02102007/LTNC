package model;

import exception.InvalidBidException;
import exception.AuctionClosedException;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class Auction implements Serializable { // đấu giá
    private final String auctionId;//id của lượt đấu giá
    private final Item item;   // vật phẩm kh thể thiếu trong đấu giá
    private final LocalDateTime startTime;  // để final bởi vì thuowngf kh thay đỏi
    private LocalDateTime endTime;
    private double currentHighestBid; // giá cao nhât hiện tại
    private Bidder currentLeader;    //người đấu thầu giá cao nhất
    private AuctionStatus status;   // trạng thái hiện tại
    private final List<BidTransaction> bidHistory;


    // Observer Pattern: danh sách người đang "theo dõi" phiên này
    private transient List<AuctionObserver> observers = new ArrayList<>();

    // ScheduledExecutor: tự động đóng phiên khi hết giờ
    private transient ScheduledExecutorService scheduler;


    public Auction(String auctionId, Item item, LocalDateTime endTime) {
        this.auctionId = auctionId;
        this.item = item;
        this.startTime = LocalDateTime.now();
        this.endTime = endTime;
        this.currentHighestBid = item.getStartingPrice();
        this.status = AuctionStatus.OPEN;
        this.bidHistory = new ArrayList<>();
    }

    // Đăng ký observer
    public synchronized void addObserver(AuctionObserver observer) {
        if (observers == null) observers = new ArrayList<>();
        observers.add(observer);
    }
    // hủy đăng ký observer
    public synchronized void removeObserver(AuctionObserver observer) {
        if (observers != null) observers.remove(observer);
    }
        // Gọi hàm này sau khi tạo đấu giá để bắt đầu đếm giờ
    public void startAuction() {
        if (status != AuctionStatus.OPEN) return;
        status = AuctionStatus.RUNNING;
        // Tính số giây còn lại đến endTime
        long delay = java.time.Duration.between(LocalDateTime.now(), endTime).getSeconds();
        if (delay <= 0) delay = 1;
        //tạo 1 luồng chuyên dùng để chạy tác vụ thgian
        scheduler = Executors.newSingleThreadScheduledExecutor();
        //đóng đấu giá khi qua thgian delay
        scheduler.schedule(this::closeAuction, delay, TimeUnit.SECONDS);
        System.out.println("Phien " + auctionId + " bat dau! Ket thuc sau " + delay + " giay.");
    }


    // synchronized = chỉ 1 thread được vào hàm này tại 1 thời điểm
    // → tránh race condition khi 2 người đặt giá cùng lúc (1 điểm đồng thời!)
    public synchronized void placeBid(Bidder bidder, double amount) throws InvalidBidException, AuctionClosedException {
        //kiểm tra lượt đấu giá còn mở không
        if (status == AuctionStatus.FINISHED || status == AuctionStatus.CANCELED) {
            throw new AuctionClosedException("Phiên đấu giá #" + auctionId + " đã kết thúc, không thể đặt giá.");
        }
        //kiểm tra số tiền đặt vào có hợp lý không nếu ít hơn giá hiện tại sẽ ném ngoại lệ
        if (amount <= currentHighestBid) {
            throw new InvalidBidException("Giá đặt của bạn hiên tại là :" + amount + "phải cao hơn giá: " + currentHighestBid);
        }
        // Cập nhật trạng thái hoạt động của phiên đấu giá, n
        if (status == AuctionStatus.OPEN) {
            status = AuctionStatus.RUNNING;
        }
        currentHighestBid = amount; // giá mới se được cập nhật
        currentLeader = bidder;

        String bidId = "BID-" + System.currentTimeMillis(); // dùng để tạo 1 id riêng cho môi lần đặt giá, duùng time vì mỗi thời điểm giá trị sẽ khác nhau
        BidTransaction tx = new BidTransaction(bidId, bidder, amount); // tạo 1 đối tượng tham giá phiên với id vừa tạo, ngời tham và số tiền
        // Notify tất cả observer ngay lập tức
        notifyBidUpdated(tx);

        System.out.println("✓ " + tx);// hiện thị thông tin , sau này sẽ học socket và thay thế
    }
    public synchronized void closeAuction() { // công đoạn kết thúc
        if( status == AuctionStatus.RUNNING || status == AuctionStatus.OPEN) {
            status = AuctionStatus.FINISHED;
            if (currentLeader != null) {// nếu tồn tại n đặt thì ->winner
                System.out.println("Phiên " + auctionId + ": Người chiến thắng là " + currentLeader.getUsername() + " với giá : " + currentHighestBid );
            }
            else {
                System.out.println("Phiên " + auctionId + ": Không có người đấu giá");
                status = AuctionStatus.CANCELED;// kh có thì canceled
            }
            // thông báo tất cả observer phiên đã đóng
            notifyAuctionClosed();
            if (scheduler != null) {
                scheduler.shutdownNow();
            }
        }
    }

    // Anti-sniping: gia hạn thêm giây nếu bid vào phút cuối.
    public synchronized void extendTime(int seconds) {
        this.endTime = endTime.plusSeconds(seconds);
        System.out.println("Phien " + auctionId + " duoc gia han them " + seconds + " giay!");
    }

    // Notify helpers — private, chỉ gọi từ bên trong
    private void notifyBidUpdated(BidTransaction tx) {
        if (observers == null) return;
        for (AuctionObserver o : observers) {
            o.onBidUpdated(this, tx);
        }
    }
    // this là đối tượng đấu giá hiện tại nhờ đó observer nhận đủ thông tin.
    private void notifyAuctionClosed() {
        if (observers == null) return;
        for (AuctionObserver o : observers) {
            o.onAuctionClosed(this);
        }
    }

    public String getAuctionId()                { return auctionId; }
    public Item getItem()                       { return item; }
    public double getCurrentHighestBid()        { return currentHighestBid; }
    public Bidder getCurrentLeader()            { return currentLeader; }
    public AuctionStatus getStatus()            { return status; }
    public List<BidTransaction> getBidHistory() { return new ArrayList<>(bidHistory); }
    public LocalDateTime getEndTime()           { return endTime; }
    public void setEndTime(LocalDateTime t)     { this.endTime = t; }
}


