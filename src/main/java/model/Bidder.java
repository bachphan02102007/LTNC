package model;

import java.io.Serializable;

public class Bidder extends User implements Serializable, WalletOwner {
    public Bidder(String id, String username, String password) {
        super(id, username, password);
    }

    public Bidder(String id, String username, String password, String phoneNumber) {
        super(id, username, password, phoneNumber);
    }

    @Override
    public boolean supportsWallet() { return true; }

    @Override
    public void displayRole() {
        System.out.println("Role: Bidder " + this.username);
    }
}
