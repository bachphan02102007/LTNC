package Main;

import model.*;
import exception.*;
import util.Factory.ItemFactory;
import util.Singleton.AuctionManager;

import java.time.LocalDateTime;

public class Main {
    public static void main(String[] args) {
        // Test Factory
        Item laptop = ItemFactory.create("ELECTRONICS", "I001", "Laptop Dell",
                "Core i7 RAM 16GB", 500.0, "12");
        System.out.println(laptop.printInfo());

        // Test Auction
        Bidder alice = new Bidder("U001", "alice", "pass123");
        Bidder bob   = new Bidder("U002", "bob",   "pass456");

        Auction auction = new Auction("A001", laptop,
                LocalDateTime.now().plusHours(2));

        // Test Singleton
        AuctionManager.getInstance().addAuction(auction);

        try {
            auction.placeBid(alice, 550.0);  // ✓ hợp lệ
            auction.placeBid(bob,   600.0);  // ✓ hợp lệ
            auction.placeBid(alice, 580.0);  // ✗ thấp hơn → InvalidBidException
        } catch (InvalidBidException e) {
            System.out.println("Lỗi hợp lệ: " + e.getMessage());
        } catch (AuctionClosedException e) {
            System.out.println("Lỗi phiên đóng: " + e.getMessage());
        }

        auction.closeAuction();
    }
}