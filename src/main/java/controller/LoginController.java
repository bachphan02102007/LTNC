package controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import model.Bidder;
import model.User;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    // Dữ liệu giả để test — tuần 9-10 sẽ thay bằng gọi Server qua Socket
    private final java.util.List<User> fakeUsers = java.util.List.of(
            new Bidder("U001", "alice", "123"),
            new Bidder("U002", "bob",   "456")
    );

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Vui long nhap day du thong tin!");
            return;
        }

        // Kiểm tra đăng nhập
        User matched = fakeUsers.stream()
                .filter(u -> u.getUsername().equals(username)
                        && u.getPassword().equals(password))
                .findFirst()
                .orElse(null);

        if (matched == null) {
            errorLabel.setText("Sai ten dang nhap hoac mat khau!");
            return;
        }

        // Đăng nhập thành công → chuyển sang màn hình danh sách
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/view/auction_list.fxml"));
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(loader.load(), 700, 500));
            stage.setTitle("Danh sach Dau gia - " + matched.getUsername());
        } catch (Exception e) {
            errorLabel.setText("Loi chuyen man hinh: " + e.getMessage());
        }
    }

    @FXML
    private void handleGoRegister() {
        // TODO tuần 8: mở màn hình đăng ký
        errorLabel.setText("Chuc nang dang ky se co o tuan 8!");
    }
}
