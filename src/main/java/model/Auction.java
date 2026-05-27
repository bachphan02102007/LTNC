package model;

import exception.InvalidBidException;
import exception.AuctionClosedException;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class Auction implements Serializable {
    private final String auctionId;
    private final Item item;
    private final LocalDateTime startTime;
    private LocalDateTime endTime;
    private double currentHighestBid;
    private Bidder currentLeader;
    private AuctionStatus status;
    private final List<BidTransaction> bidHistory;
    private String sellerUsername;
    private boolean paid;

    private transient List<AuctionObserver> observers = new ArrayList<>();
    private transient ScheduledExecutorService scheduler;
    private transient ReentrantLock lock = new ReentrantLock();

    public Auction(String auctionId, Item item, LocalDateTime endTime) {
        this(auctionId, item, endTime, null);
    }

    public Auction(String auctionId, Item item, LocalDateTime endTime, String sellerUsername) {
        this.auctionId = auctionId;
        this.item = item;
        this.startTime = LocalDateTime.now();
        this.endTime = endTime;
        this.currentHighestBid = item.getStartingPrice();
        this.status = AuctionStatus.OPEN;
        this.bidHistory = new ArrayList<>();
        this.sellerUsername = sellerUsername;
        this.paid = false;
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        observers = new ArrayList<>();
        lock = new ReentrantLock();
        scheduler = null;
        if (status == null) status = AuctionStatus.OPEN;
    }

    public synchronized void addObserver(AuctionObserver observer) {
        if (observers == null) observers = new ArrayList<>();
        observers.add(observer);
    }


    public void startAuction() {
        lock.lock();
        try {
            if (status != AuctionStatus.OPEN) return;
            status = AuctionStatus.RUNNING;
            scheduleCloseLocked();
        } finally {
            lock.unlock();
        }
    }


    public void resumeScheduler() {
        lock.lock();
        try {
            if (status == AuctionStatus.PAID || status == AuctionStatus.CANCELED || status == AuctionStatus.FINISHED) return;
            if (LocalDateTime.now().isAfter(endTime)) {
                status = currentLeader == null ? AuctionStatus.CANCELED : AuctionStatus.FINISHED;
                notifyAuctionClosed();
                return;
            }
            if (status == AuctionStatus.OPEN) status = AuctionStatus.RUNNING;
            scheduleCloseLocked();
        } finally {
            lock.unlock();
        }
    }

    private void scheduleCloseLocked() {
        long delay = java.time.Duration.between(LocalDateTime.now(), endTime).getSeconds();
        if (delay <= 0) delay = 1;
        if (scheduler != null) scheduler.shutdownNow();
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.schedule(this::closeAuction, delay, TimeUnit.SECONDS);
        System.out.println("Phien " + auctionId + " bat dau! Ket thuc sau " + delay + " giay.");
    }

    public void placeBid(Bidder bidder, double amount) throws InvalidBidException, AuctionClosedException {
        lock.lock();
        try {
            if (status == AuctionStatus.FINISHED || status == AuctionStatus.CANCELED || status == AuctionStatus.PAID
                    || LocalDateTime.now().isAfter(endTime)) {
                closeAuction();
                throw new AuctionClosedException("Phiên đấu giá #" + auctionId + " đã kết thúc, không thể đặt giá.");
            }
            if (amount <= currentHighestBid) {
                throw new InvalidBidException("Giá đặt " + amount + " phải cao hơn giá hiện tại " + currentHighestBid);
            }
            if (status == AuctionStatus.OPEN) status = AuctionStatus.RUNNING;
            currentHighestBid = amount;
            currentLeader = bidder;
            BidTransaction tx = new BidTransaction("BID-" + System.currentTimeMillis(), bidder, amount);
            bidHistory.add(tx);
            notifyBidUpdated(tx);
            System.out.println("✓ " + tx);
        } finally {
            lock.unlock();
        }
    }

    public void closeAuction() {
        lock.lock();
        try {
            if (status == AuctionStatus.RUNNING || status == AuctionStatus.OPEN) {
                status = currentLeader == null ? AuctionStatus.CANCELED : AuctionStatus.FINISHED;
                notifyAuctionClosed();
                if (scheduler != null) scheduler.shutdownNow();
            }
        } finally {
            lock.unlock();
        }
    }

    public void cancelAuction() {
        lock.lock();
        try {
            if (status != AuctionStatus.PAID) {
                status = AuctionStatus.CANCELED;
                if (scheduler != null) scheduler.shutdownNow();
                notifyAuctionClosed();
            }
        } finally {
            lock.unlock();
        }
    }

    public void markPaid() {
        lock.lock();
        try {
            paid = true;
            status = AuctionStatus.PAID;
        } finally {
            lock.unlock();
        }
    }



     //Dùng phía client để phục hồi snapshot nhận từ server mà không phát sinh observer/broadcast.

    public void restoreSnapshot(double currentHighestBid, String leaderUsername,
                                AuctionStatus status, List<BidTransaction> history) {
        lock.lock();
        try {
            this.currentHighestBid = currentHighestBid;
            this.currentLeader = leaderUsername == null || leaderUsername.isBlank()
                    ? null
                    : new Bidder("", leaderUsername, "");
            if (status != null) this.status = status;
            this.bidHistory.clear();
            if (history != null) this.bidHistory.addAll(history);
        } finally {
            lock.unlock();
        }
    }

    public void extendTime(int seconds) {
        lock.lock();
        try {
            this.endTime = endTime.plusSeconds(seconds);
            if (status == AuctionStatus.RUNNING) scheduleCloseLocked();
        } finally {
            lock.unlock();
        }
    }

    private void notifyBidUpdated(BidTransaction tx) {
        if (observers == null) return;
        for (AuctionObserver o : observers) o.onBidUpdated(this, tx);
    }

    private void notifyAuctionClosed() {
        if (observers == null) return;
        for (AuctionObserver o : observers) o.onAuctionClosed(this);
    }

    public String getAuctionId() { return auctionId; }
    public Item getItem() { return item; }
    public LocalDateTime getStartTime() { return startTime; }
    public double getCurrentHighestBid() { return currentHighestBid; }
    public Bidder getCurrentLeader() { return currentLeader; }
    public AuctionStatus getStatus() { return status; }
    public List<BidTransaction> getBidHistory() { return new ArrayList<>(bidHistory); }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime t) { this.endTime = t; }
    public String getSellerUsername() { return sellerUsername; }
    public void setSellerUsername(String sellerUsername) { this.sellerUsername = sellerUsername; }
    public boolean isPaid() { return paid; }
}
