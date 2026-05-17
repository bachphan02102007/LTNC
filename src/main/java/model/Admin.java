package model;

public class Admin extends User {

    public Admin(String id, String username, String password) {
        super(id, username, password);
    }

    public Admin(String id, String username, String password, String phoneNumber) {
        super(id, username, password, phoneNumber);
    }

    @Override
    public void displayRole() {
        System.out.println("Role: Admin (Quản trị viên) - " + this.username);
    }
}
