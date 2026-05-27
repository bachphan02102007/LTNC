package model;

import java.io.Serializable;
import java.util.Objects;

public class Bidder extends User implements Serializable, WalletOwner {
    public Bidder(String id, String username, String password) {
        super(id, username, password);
    }

    public Bidder(String id, String username, String password, String phoneNumber) {
        super(id, username, password, phoneNumber);
    }

    @Override
    public boolean supportsWallet() {
        return true;
    }

    @Override
    public void displayRole() {
        System.out.println("Role: Bidder " + this.username);
    }

    public int getAvailableBalance() {
        return 0;
    }

    public int getLockedBalance() {
        return 0;
    }

    public void lockFunds(int i) {
    }
    @Override
    public boolean equals(Object o) {
        // 1. Nếu cùng trỏ vào 1 ô nhớ thì chắc chắn bằng nhau
        if (this == o) return true;
        // 2. Nếu đối tượng truyền vào bị null hoặc khác Class thì không bằng nhau
        if (o == null || getClass() != o.getClass()) return false;
        // 3. Ép kiểu về Bidder và so sánh thuộc tính ID (Giả định thuộc tính ID của bạn tên là 'id')
        Bidder bidder = (Bidder) o;
        return Objects.equals(this.id, bidder.id);
    }
    @Override
    public int hashCode() {
        // Đồng bộ mã băm dựa trên thuộc tính ID
        return Objects.hash(id);
    }
}
