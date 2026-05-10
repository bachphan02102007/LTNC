package controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import model.Seller;
import model.User;
import util.SessionManager;
import util.Singleton.UserManager;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Vui long nhap day du thong tin!");
            return;
        }

        UserManager.getInstance().authenticate(username, password)
                .ifPresentOrElse(
                        user -> {
                            // Lưu user vào session
                            SessionManager.getInstance().login(user);
                            try {
                                Stage stage = (Stage) usernameField.getScene().getWindow();
                                if (user instanceof Seller) {
                                    // Seller → seller dashboard
                                    goSeller(stage, (Seller) user);
                                } else {
                                    // Bidder / Admin → danh sách phiên
                                    goAuctionList(stage);
                                }
                            } catch (Exception e) {
                                errorLabel.setText("Loi: " + e.getMessage());
                            }
                        },
                        () -> errorLabel.setText("Sai ten dang nhap hoac mat khau!")
                );
    }

    private void goAuctionList(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/view/auction_list.fxml"));
        stage.setScene(new Scene(loader.load(), 700, 500));
        stage.setTitle("Danh sach Phien Dau Gia");
        stage.show();
    }

    private void goSeller(Stage stage, Seller seller) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/view/seller_dashboard.fxml"));
        stage.setScene(new Scene(loader.load(), 800, 600));
        stage.setTitle("Quan ly San pham - Seller");

        SellerController ctrl = loader.getController();
        ctrl.initData(seller, SessionManager.getInstance().getSocketClient());

        stage.show();
    }

    @FXML
    private void handleGoRegister() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/view/register.fxml"));
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(loader.load(), 400, 420));
            stage.setTitle("Dang ky tai khoan");
        } catch (Exception e) {
            errorLabel.setText("Loi: " + e.getMessage());
        }
    }
}
