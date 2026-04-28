package main;

import model.*;
import exception.*;
import util.Factory.ItemFactory;
import util.Singleton.AuctionManager;

import java.time.LocalDateTime;

public class Main {
    public static void main(String[] args) throws InterruptedException {

        // Tạo sản phẩm
        Item laptop = ItemFactory.create("ELECTRONICS", "I001",
                "Laptop Dell", "Core i7", 500.0, "12");

        // Tạo phiên đấu giá kéo dài 5 giây để test
        Auction auction = new Auction("A001", laptop,
                LocalDateTime.now().plusSeconds(5));

        // Đăng ký 2 observer — giả lập 2 client đang xem
        auction.addObserver(new ConsoleObserver("Client-Alice"));
        auction.addObserver(new ConsoleObserver("Client-Bob"));

        // Lưu vào AuctionManager
        AuctionManager.getInstance().addAuction(auction);

        // Bắt đầu phiên
        auction.startAuction();

        // Tạo bidder và đặt giá
        Bidder alice = new Bidder("U001", "alice", "pass");
        Bidder bob   = new Bidder("U002", "bob",   "pass");

        try {
            auction.placeBid(alice, 550.0);
            Thread.sleep(500);
            auction.placeBid(bob, 620.0);
            Thread.sleep(500);
            auction.placeBid(alice, 580.0); // băt lỗi giá thấp hơn
        } catch (InvalidBidException e) {
            System.out.println("LOI: " + e.getMessage());
        } catch (AuctionClosedException e) {
            System.out.println("LOI: " + e.getMessage());
        }

        // Chờ phiên tự đóng sau 5 giây
        Thread.sleep(6000);
        System.out.println("Ket thuc chuong trinh.");
    }
}