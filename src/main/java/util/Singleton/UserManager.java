package util.Singleton;


import exception.AuthenticationException;
import model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserManager {

    private static UserManager instance;
    private final List<User> users = new ArrayList<>();

    private UserManager() {}

    public static synchronized UserManager getInstance() {
        if (instance == null) instance = new UserManager();
        return instance;
    }

    public void addUser(User user) throws AuthenticationException {
        boolean exists = users.stream()
                .anyMatch(u -> u.getUsername().equals(user.getUsername()));
        if (exists) throw new AuthenticationException(
                "Ten dang nhap '" + user.getUsername() + "' da ton tai!");
        users.add(user);
    }

    public Optional<User> authenticate(String username, String password) {
        return users.stream()
                .filter(u -> u.getUsername().equals(username)
                        && u.getPassword().equals(password))
                .findFirst();
    }

    public List<User> getAllUsers() { return new ArrayList<>(users); }
}