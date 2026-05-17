package util.Singleton;

import model.Auction;
import model.AuctionStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AuctionManager {
    private static AuctionManager instance;
    private final List<Auction> auctions;

    private AuctionManager() {
        auctions = new ArrayList<>();
    }

    public static synchronized AuctionManager getInstance() {
        if(instance == null ) {
            instance = new AuctionManager();
        }
        return instance;
    }

    public synchronized void addAuction(Auction auction) {
        if (findById(auction.getAuctionId()).isEmpty()) auctions.add(auction);
    }

    public synchronized Optional<Auction> findById(String id) {
        return auctions.stream().filter(a -> a.getAuctionId().equals(id)).findFirst();
    }

    public synchronized List<Auction> getAllAuctions() {
        return auctions.stream()
                .sorted((a, b) -> b.getStartTime().compareTo(a.getStartTime()))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public synchronized List<Auction> getRunningAuctions() {
        return auctions.stream()
                .filter(a -> a.getStatus() == AuctionStatus.RUNNING)
                .collect(Collectors.toList());
    }

    public synchronized List<Auction> findBySeller(String sellerUsername) {
        return auctions.stream()
                .filter(a -> sellerUsername != null && sellerUsername.equals(a.getSellerUsername()))
                .sorted((a, b) -> b.getStartTime().compareTo(a.getStartTime()))
                .collect(Collectors.toList());
    }

    public synchronized boolean cancelAuction(String auctionId) {
        Optional<Auction> auction = findById(auctionId);
        auction.ifPresent(Auction::cancelAuction);
        return auction.isPresent();
    }

    public synchronized boolean removeAuction(String auctionId) {
        return auctions.removeIf(a -> a.getAuctionId().equals(auctionId));
    }

    public synchronized void clearAuctions() { auctions.clear(); }

    public synchronized List<Auction> getVisibleAuctions() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        return auctions.stream()
                .filter(a -> a.getEndTime().plusDays(1).isAfter(now))
                .sorted((a, b) -> b.getStartTime().compareTo(a.getStartTime()))
                .collect(Collectors.toList());
    }
}
