// exception/InvalidBidException.java
package exception;

public class InvalidBidException extends Exception {// ngoại lệ giá đấu thầu kh hợp lệ
    public InvalidBidException(String message) {
        super(message);
    }
}