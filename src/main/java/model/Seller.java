package model;

import java.io.Serializable;

public class Seller extends User implements Serializable, WalletOwner {
    public Seller(String id, String username, String password) {
        super(id, username, password);
    }

    public Seller(String id, String username, String password, String phoneNumber) {
        super(id, username, password, phoneNumber);
    }

    @Override
    public boolean supportsWallet() { return true; }

    @Override
    public void displayRole() {
        System.out.println("Role: Seller " + this.username);
    }
}
