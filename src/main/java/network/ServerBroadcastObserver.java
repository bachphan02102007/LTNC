package network;

import model.Auction;
import model.AuctionObserver;
import model.BidTransaction;
import model.User;
import util.Singleton.UserManager;
import util.Singleton.AuctionManager;
import util.DataStorage;

/**
 * Observer nối Auction model với server broadcast.
 * Khi phiên đóng, server tự trừ ví người thắng và chuyển PAID nếu đủ tiền.
 */
public class ServerBroadcastObserver implements AuctionObserver {

    @Override
    public void onBidUpdated(Auction auction, BidTransaction tx) {
        String msg = "GIA_MOI:"
                + auction.getAuctionId() + ":"
                + tx.getAmount() + ":"
                + tx.getBidder().getUsername();
        AuctionServer.broadcastAll(msg);
    }

    @Override
    public void onAuctionClosed(Auction auction) {
        String winner = auction.getCurrentLeader() != null
                ? auction.getCurrentLeader().getUsername()
                : "KHONG_AI";
        String seller = auction.getSellerUsername() != null ? auction.getSellerUsername() : "";

        // Đồng bộ user mới nhất từ file trước khi trừ ví.
        java.util.List<User> latestUsers = DataStorage.loadUsers();
        if (!latestUsers.isEmpty()) UserManager.getInstance().loadFrom(latestUsers);

        String winnerPhone = "";
        String autoPayStatus = "NO_WINNER";
        if (auction.getCurrentLeader() != null) {
            java.util.Optional<User> winnerUser = UserManager.getInstance().findByUsername(winner);
            winnerPhone = winnerUser.map(User::getPhoneNumber).orElse(auction.getCurrentLeader().getPhoneNumber());
            try {
                if (winnerUser.isPresent()) {
                    winnerUser.get().withdraw(auction.getCurrentHighestBid());
                    auction.markPaid();
                    autoPayStatus = "AUTO_PAID";
                    DataStorage.saveUsers(UserManager.getInstance().getAllUsers());
                    AuctionServer.broadcastAll("WALLET_CHANGED:" + winner + ":" + winnerUser.get().getWalletBalance());
                } else {
                    autoPayStatus = "PAY_FAIL_USER_NOT_FOUND";
                }
            } catch (Exception ex) {
                autoPayStatus = "PAY_FAIL_" + ex.getMessage().replace(':', ' ');
            }
        }

        String sellerPhone = UserManager.getInstance().findByUsername(seller)
                .map(User::getPhoneNumber)
                .orElse("");
        double finalPrice = auction.getCurrentHighestBid();

        String msg = "AUCTION_CLOSED:"
                + auction.getAuctionId() + ":"
                + winner + ":"
                + finalPrice + ":"
                + seller + ":"
                + sellerPhone + ":"
                + winnerPhone + ":"
                + autoPayStatus;
        AuctionServer.broadcastAll(msg);
        if ("AUTO_PAID".equals(autoPayStatus)) {
            AuctionServer.broadcastAll("AUCTION_PAID:" + auction.getAuctionId() + ":" + winner);
        }
        DataStorage.saveAuctions(AuctionManager.getInstance().getAllAuctions());

        System.out.println("[Server] Phien " + auction.getAuctionId()
                + " da ket thuc. Winner: " + winner
                + " | Gia: " + finalPrice
                + " | Seller: " + seller + " - " + sellerPhone
                + " | AutoPay=" + autoPayStatus);
    }
}
