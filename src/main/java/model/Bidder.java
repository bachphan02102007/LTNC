package model;

public class Bidder extends User {
    public Bidder(String id, String username, String password) {
        super(id, username, password);
    }

    @Override
    public void displayRole() {
        System.out.println("Role: Bidder " + this.username);
    }
}
