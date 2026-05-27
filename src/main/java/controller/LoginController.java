package controller;
//Vai trò của UserManager
//UserManager là nơi quản lý toàn bộ danh sách user trong hệ thống.
//Nó cho phép:
//Thêm user mới (addUser).
//Xác thực đăng nhập (authenticate).
//Lấy danh sách tất cả user (getAllUsers).
// Nó giống như một “cơ sở dữ liệu trong bộ nhớ” cho toàn bộ người dùng.

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import model.Admin;
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
                            SessionManager.getInstance().login(user);
                            try {
                                Stage stage = (Stage) usernameField.getScene().getWindow();
                                if (user instanceof Seller) {
                                    goSeller(stage, (Seller) user);
                                } else if (user instanceof Admin) {
                                    goAdmin(stage, (Admin) user);
                                } else {
                                    try {
                                        SessionManager.getInstance()
                                                .getSocketClient()
                                                .connect(user.getUsername(), "BIDDER", msg -> {
                                                    System.out.println("[Bidder socket] " + msg);
                                                });
                                    } catch (Exception e) {
                                        errorLabel.setText("Không thể kết nối server: " + e.getMessage());
                                        return;
                                    }

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
        stage.setScene(new Scene(loader.load()));
        stage.setTitle("Danh sach Phien Dau Gia");
        stage.show();
    }

    private void goAdmin(Stage stage, Admin admin) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/view/admin_dashboard.fxml"));
        stage.setScene(new Scene(loader.load()));
        stage.setTitle("Quan tri he thong - Admin");
        AdminController ctrl = loader.getController();
        ctrl.initData(admin, SessionManager.getInstance().getSocketClient());
        stage.show();
    }

    private void goSeller(Stage stage, Seller seller) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/view/seller_dashboard.fxml"));
        stage.setScene(new Scene(loader.load()));
        stage.setTitle("Quan ly San pham - Seller");

        SellerController ctrl = loader.getController();
        //Truyền thông tin người bán đang đăng nhập và socket client sang màn hình Seller Dashboard.
        ctrl.initData(seller, SessionManager.getInstance().getSocketClient());

        stage.show();
    }

    @FXML
    private void handleGoRegister() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/view/register.fxml"));
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));
            stage.setTitle("Dang ky tai khoan");
        } catch (Exception e) {
            errorLabel.setText("Loi: " + e.getMessage());
        }
    }
}
