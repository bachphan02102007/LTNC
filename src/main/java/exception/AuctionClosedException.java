// exception/AuctionClosedException.java
package exception;

public class AuctionClosedException extends Exception { // ngoại leje khi 1 người tham gia phiên đấu giá đã đóng
    public AuctionClosedException(String message) {
        super(message);
    }
}