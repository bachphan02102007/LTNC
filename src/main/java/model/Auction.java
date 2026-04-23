package model;

import exception.InvalidBidException;
import exception.AuctionClosedException;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


public class Auction implements Serializable { // đấu giá
    private final String auctionId;//id của lượt đấu giá
    private final Item item;   // vật phẩm kh thể thiếu trong đấu giá
    private final LocalDateTime startTime;  // để final bởi vì thuowngf kh thay đỏi
    private LocalDateTime endTime;
    private double currentHighestBid; // giá cao nhât hiện tại
    private Bidder currentLeader;    //người đấu thầu giá cao nhất
    private AuctionStatus status;   // trạng thái hiện tại
    private final List<BidTransaction> bidHistory;

    public Auction(String auctionId, Item item, LocalDateTime endTime) {
        this.auctionId = auctionId;
        this.item = item;
        this.startTime = LocalDateTime.now();
        this.endTime = endTime;
        this.currentHighestBid = item.getStartingPrice();
        this.status = AuctionStatus.OPEN;
        this.bidHistory = new ArrayList<>();
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
        bidHistory.add(tx);// lưu lại lịch sử của phiên đấu giá

        System.out.println("✓ " + tx);// hiện thị thông tin , sau này sẽ học socket và thay thế
    }
    public synchronized void closeAution() { // công đoạn kết thúc
        if( status == AuctionStatus.RUNNING || status == AuctionStatus.OPEN) {
            status = AuctionStatus.FINISHED;
            if (currentLeader != null) {// nếu tồn tại n đặt thì ->winner
                System.out.println("Phiên " + auctionId + ": Người chiến thắng là " + currentLeader.getUsername() + " với giá : " + currentHighestBid );
            }
            else {
                System.out.println("Phiên " + auctionId + ": Không có người đấu giá");
                status = AuctionStatus.CANCELED;// kh có thì canceled
            }
        }
    }

    public String getAuctionId() {
        return auctionId;
    }

    public Item getItem() {
        return item;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public double getCurrentHighestBid() {
        return currentHighestBid;
    }

    public Bidder getCurrentLeader() {
        return currentLeader;
    }

    public AuctionStatus getStatus() {
        return status;
    }

    public List<BidTransaction> getBidHistory() {
        return new ArrayList<>(bidHistory);
    }
}


