package model;


 // Interface tách riêng hành vi ví tiền khỏi User chung.
 //Chỉ những vai trò thật sự thanh toán (Bidder) mới nên implement interface này.

public interface WalletOwner {
    double getWalletBalance();
    void setWalletBalance(double walletBalance);
    void deposit(double amount);
    void withdraw(double amount);
}
