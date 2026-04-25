package util.Singleton;

import model.Auction;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
public class AuctionManager { // người quản lý đấu giá
    // Singleton: chỉ tồn tại 1 instance duy nhất trong toàn bộ chương trình
    private static AuctionManager instance;
    private final List<Auction> auctions;


    // private constructor → không ai new AuctionManager() từ bên ngoài được
    private AuctionManager() {
        auctions = new ArrayList<>();
    }

    // synchronized để thread-safe khi nhiều thread cùng gọi lần đầu
    //static là biến thuộc về class nhờ đó chương trình chỉ có 1 instance
    //Nếu không static, mỗi lần bạn tạo AuctionManager mới thì sẽ có một biến instance riêng biệt, phá vỡ nguyên tắc Singleton.
    public static synchronized AuctionManager getInstance() {
        if(instance == null ) {
            instance = new AuctionManager();   // chỉ được tạo 1 đói tương duy nhất
        }
        return instance;
    }
    public void addAuction(Auction auction) {
        auctions.add(auction);
    }
    public Optional<Auction> findById(String id) { // pt công khai tra về Optional<Auction>.
        return auctions.stream().filter(a -> a.getAuctionId().equals(id))
                .findFirst(); // lọc các phần tử trong stream giữ lại phiên đấu giá giống với giá tr id truyền vào
    }                         // ấy cái đầu tiên nếu thấy

    //Lấy toàn bộ danh sách phiên đấu giá.
    //Trả về bản sao (new ArrayList<>(...)) để tránh việc bên ngoài sửa trực tiếp danh sách gốc
    public List<Auction> getAllAuctions() {
        return new ArrayList<>(auctions);
    }

    //Lọc ra các phiên đấu giá đang ở trạng thái RUNNING.
    //Sử dụng Java Stream API để duyệt và lọc.
    public List<Auction> getRunningAuctions() {
        return auctions.stream()
                .filter(a -> a.getStatus().name().equals("RUNNING"))
                .collect(java.util.stream.Collectors.toList());
    }
}