package model;

import java.io.Serializable;

public class Bidder extends User implements Serializable {
    public Bidder(String id, String username, String password) {
        super(id, username, password);
    }

    @Override
    public void displayRole() {
        System.out.println("Role: Bidder " + this.username);
    }
}
