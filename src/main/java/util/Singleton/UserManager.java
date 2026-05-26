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

    public synchronized void addUser(User user) throws AuthenticationException {
        boolean exists = users.stream()
                .anyMatch(u -> u.getUsername().equalsIgnoreCase(user.getUsername()));
        if (exists) throw new AuthenticationException(
                "Ten dang nhap '" + user.getUsername() + "' da ton tai!");
        users.add(user);
    }

    public synchronized Optional<User> authenticate(String username, String password) {
        return users.stream()
                .filter(u -> u.isActive())
                .filter(u -> u.getUsername().equals(username)
                        && u.getPassword().equals(password))
                .findFirst();
    }

    public synchronized Optional<User> findByUsername(String username) {
        return users.stream()
                .filter(u -> u.getUsername().equals(username))
                .findFirst();
    }

    public synchronized boolean deactivateUser(String username) {
        Optional<User> user = findByUsername(username);
        user.ifPresent(u -> u.setActive(false));
        return user.isPresent();
    }

    public synchronized boolean activateUser(String username) {
        Optional<User> user = findByUsername(username);
        user.ifPresent(u -> u.setActive(true));
        return user.isPresent();
    }

    public synchronized List<User> getAllUsers() { return new ArrayList<>(users); }

    public synchronized void loadFrom(List<User> latestUsers) {
        users.clear();
        users.addAll(latestUsers);
    }

    public synchronized void syncUser(User latest) {
        if (latest == null) return;
        findByUsername(latest.getUsername()).ifPresentOrElse(existing -> {
            existing.setFullName(latest.getFullName());
            existing.setPhoneNumber(latest.getPhoneNumber());
            existing.setPassword(latest.getPassword());
            existing.setWalletBalance(latest.getWalletBalance());
            existing.setActive(latest.isActive());
        }, () -> users.add(latest));
    }

    public synchronized void clearUsers() { users.clear(); }

    public void clearAll() {

    }
}
