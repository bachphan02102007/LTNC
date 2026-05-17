package model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class BidTransaction implements Serializable {
    private static final long serialVersionUID = 2L;
    private final String bidId;
    private final Bidder bidder;
    private final double amount;
    private final LocalDateTime timestamp;

    public BidTransaction(String bidId, Bidder bidder, double amount) {
        this(bidId, bidder, amount, LocalDateTime.now());
    }

    public BidTransaction(String bidId, Bidder bidder, double amount, LocalDateTime timestamp) {
        this.bidId = bidId;
        this.bidder = bidder;
        this.amount = amount;
        this.timestamp = timestamp == null ? LocalDateTime.now() : timestamp;
    }

    public String getBidId()           { return bidId; }
    public Bidder getBidder()          { return bidder; }
    public double getAmount()          { return amount; }
    public LocalDateTime getTimestamp(){ return timestamp; }

    @Override
    public String toString() {
        return "[" + timestamp + "] " + bidder.getUsername() + " đặt " + amount + " VNĐ";
    }
}
