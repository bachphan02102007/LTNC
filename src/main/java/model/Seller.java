package model;

import java.io.Serializable;

public class Seller extends User implements Serializable {
    public Seller(String id, String username, String password) {
        super(id, username, password); // constructor tuw lop cha
    }

    @Override
    public void displayRole() {
        System.out.println("Role: Seller " + this.username);
    }
}
