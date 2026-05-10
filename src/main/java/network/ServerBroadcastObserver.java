package network;

import model.Auction;
import model.AuctionObserver;
import model.BidTransaction;

/**
 * Observer kết nối Auction model với AuctionServer broadcast.
 * Mỗi Auction khi được tạo/load sẽ có 1 observer này để:
 *  - Khi có bid mới → broadcast GIA_MOI đến tất cả client
 *  - Khi phiên kết thúc → broadcast AUCTION_CLOSED đến tất cả client
 *
 * Được thêm vào mỗi Auction trong ClientHandler.handleAddItem()
 * và trong AuctionServer khi load từ file.
 */
public class ServerBroadcastObserver implements AuctionObserver {

    @Override
    public void onBidUpdated(Auction auction, BidTransaction tx) {
        // Broadcast giá mới đến toàn bộ client
        // Format: GIA_MOI:auctionId:amount:bidder
        String msg = "GIA_MOI:"
                + auction.getAuctionId() + ":"
                + tx.getAmount() + ":"
                + tx.getBidder().getUsername();
        AuctionServer.broadcastAll(msg);
    }

    @Override
    public void onAuctionClosed(Auction auction) {
        // Broadcast kết thúc phiên đến toàn bộ client
        // Format: AUCTION_CLOSED:auctionId:winner:finalPrice
        String winner = auction.getCurrentLeader() != null
                ? auction.getCurrentLeader().getUsername()
                : "KHONG_AI";
        double finalPrice = auction.getCurrentHighestBid();

        String msg = "AUCTION_CLOSED:"
                + auction.getAuctionId() + ":"
                + winner + ":"
                + finalPrice;
        AuctionServer.broadcastAll(msg);

        System.out.println("[Server] Phien " + auction.getAuctionId()
                + " da ket thuc. Winner: " + winner
                + " | Gia: " + finalPrice);
    }
}
