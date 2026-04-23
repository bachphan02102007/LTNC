package model;

public abstract class User {
    protected String id;          // Dùng protected để class con có thể truy cập
    protected String username;
    protected String password;
    public User (String id, String username, String password) {
        this.id = id;
        this.username = username;
        this.password = password;
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
    public abstract void displayRole(); // pt tt cho lớp con tự nên vai trò
}
