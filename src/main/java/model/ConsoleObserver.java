package model;

public class ConsoleObserver implements AuctionObserver {
    private final String name;

    public ConsoleObserver(String name) {
        this.name = name;
    }

    @Override //ghi đè để thông báo
    public void onBidUpdated(Auction auction, BidTransaction latestBid) {
        System.out.println("[" + name + "]" + "Thông báo: " + latestBid.getBidder().getUsername()
                + " vua dat $" + latestBid.getAmount()
                + " cho phien " + auction.getAuctionId());
    }

    @Override
    public void onAuctionClosed(Auction auction) {
        System.out.println("[" + name + "] PHIEN " + auction.getAuctionId()
                + " DA DONG! Nguoi thang: "
                + (auction.getCurrentLeader() != null
                ? auction.getCurrentLeader().getUsername() + " - $" + auction.getCurrentHighestBid()
                : "Khong co"));
    }
}
