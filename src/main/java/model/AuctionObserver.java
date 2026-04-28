package model;

// Interface Observer — bất kỳ ai muốn nhận thông báo đấu giá mới đều implements cái này
public interface AuctionObserver {
    //Được gọi khi có bid mới, nhận các thông tin mới như đấu giá
    void onBidUpdated(Auction auction, BidTransaction latestBid);
    //Được gọi khi phiên đấu giá kết thúc Observer sẽ biết phiên nào đã đóng và có thể xử lý
    void onAuctionClosed(Auction auction);
}