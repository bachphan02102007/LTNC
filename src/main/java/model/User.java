package model;

import java.io.Serializable;

public abstract class User implements Serializable {
    private static final long serialVersionUID = 2L;

    protected String id;
    protected String username;
    protected String password;
    protected String fullName;
    protected String phoneNumber;
    protected double walletBalance;
    protected boolean active = true;

    public User(String id, String username, String password) {
        this(id, username, password, "");
    }

    public User(String id, String username, String password, String phoneNumber) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.fullName = username;
        this.phoneNumber = phoneNumber == null ? "" : phoneNumber.trim();
        this.walletBalance = 0;
    }

    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getFullName() { return fullName == null || fullName.isBlank() ? username : fullName; }
    public String getPhoneNumber() { return phoneNumber == null ? "" : phoneNumber; }
    public boolean isActive() { return active; }

    public void setPassword(String password) {
        if (password == null) {
            throw new IllegalArgumentException("Password không được phép mang giá trị null");
        }
        if (!password.isBlank()) {
            this.password = password;
        }
    }

    public void setFullName(String fullName) {
        if (fullName != null && !fullName.isBlank()) this.fullName = fullName.trim();
    }

    public void setPhoneNumber(String phoneNumber) {
        if (phoneNumber != null) this.phoneNumber = phoneNumber.trim();
    }

    public void setActive(boolean active) { this.active = active; }

    /** Mặc định user không có ví. Bidder override thành true. */
    public boolean supportsWallet() { return false; }

    public double getWalletBalance() { return walletBalance; }

    public void setWalletBalance(double walletBalance) {
        if (!supportsWallet()) {
            this.walletBalance = 0;
            return;
        }
        if (walletBalance < 0) throw new IllegalArgumentException("So du vi khong duoc am");
        this.walletBalance = walletBalance;
    }

    public void deposit(double amount) {
        if (!supportsWallet()) throw new IllegalStateException("Vai tro nay khong su dung vi tien");
        if (amount <= 0) throw new IllegalArgumentException("So tien nap phai lon hon 0");
        walletBalance += amount;
    }

    public void withdraw(double amount) {
        if (!supportsWallet()) throw new IllegalStateException("Vai tro nay khong su dung vi tien");
        if (amount <= 0) throw new IllegalArgumentException("So tien rut phai lon hon 0");
        if (walletBalance < amount) throw new IllegalArgumentException("So du vi khong du");
        walletBalance -= amount;
    }

    public String getRoleName() {
        return getClass().getSimpleName();
    }

    public abstract void displayRole();
}
