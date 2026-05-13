package util;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

// Dùng Serialization để lưu object Java xuống file và đọc lại
public class DataStorage {

    private static final String DATA_DIR  = "data/";
    private static final String USER_FILE = DATA_DIR + "users.dat";
    private static final String AUCTION_FILE = DATA_DIR + "auctions.dat";

    // Lưu danh sách object xuống file
    public static <T> void saveList(List<T> list, String filename) {
        // Tạo thư mục data/ nếu chưa có
        new File(DATA_DIR).mkdirs();

        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream(filename))) {
            oos.writeObject(list);
            System.out.println("Da luu du lieu vao: " + filename);
        } catch (IOException e) {
            System.err.println("Loi luu du lieu: " + e.getMessage());
        }
    }

    // Đọc danh sách object từ file
    @SuppressWarnings("unchecked")
    public static <T> List<T> loadList(String filename) {
        File file = new File(filename);
        if (!file.exists()) {
            System.out.println("File chua ton tai, tra ve danh sach rong: " + filename);
            return new ArrayList<>();
        }

        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream(filename))) {
            return (List<T>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Loi doc du lieu: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // giúp methods tiện dụng
    public static void saveUsers(List<?> users) {
        saveList(users, USER_FILE);
    }

    public static <T> List<T> loadUsers() {
        return loadList(USER_FILE);
    }

    public static void saveAuctions(List<?> auctions) {
        saveList(auctions, AUCTION_FILE);
    }

    public static <T> List<T> loadAuctions() {
        return loadList(AUCTION_FILE);
    }
}