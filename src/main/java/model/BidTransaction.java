package model;

import java.io.Serializable; // Serializable là một Marker Interface (Interface đánh dấu -
// tức là một interface trống, không chứa bất kỳ phương thức nào)
import java.time.LocalDateTime;

// Serializable để sau này gửi qua Socket dễ dàng
public class BidTransaction implements Serializable { //Tạo một lớp công khai tên là BidTransaction
    // và cho phép nó được biến thành chuỗi byte (để gửi qua Socket).
    private final String bidId;
    private final Bidder bidder;
    private final double amount;
    private final LocalDateTime timestamp;

    public BidTransaction(String bidId, Bidder bidder, double amount) {
        this.bidId = bidId;
        this.bidder = bidder;        // hàm này gọi khi tạo 1 giao dịch mới bằng new
        this.amount = amount;        // nhận vào những thông tin id , tt n đấu giá, so dư
        this.timestamp = LocalDateTime.now();
    }

    public String getBidId()           { return bidId; }
    public Bidder getBidder()          { return bidder; }
    public double getAmount()          { return amount; }
    public LocalDateTime getTimestamp(){ return timestamp; }

    @Override
    public String toString() {
        return "[" + timestamp + "] " + bidder.getUsername() + " đặt $" + amount;
    }// in thông tin của 1 đối tượng bidtransaction
}