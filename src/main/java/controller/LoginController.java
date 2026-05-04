package controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import util.Singleton.UserManager;



public class LoginController {

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label errorLabel;

    // Dữ liệu giả để test — tuần 9-10 sẽ thay bằng gọi Server qua Socket


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
                            try {
                                FXMLLoader loader = new FXMLLoader(
                                        getClass().getResource("/view/auction_list.fxml"));
                                Stage stage = (Stage) usernameField.getScene().getWindow();
                                stage.setScene(new Scene(loader.load(), 700, 500));
                            } catch (Exception e) {
                                errorLabel.setText("Loi: " + e.getMessage());
                            }
                        },
                        () -> errorLabel.setText("Sai ten dang nhap hoac mat khau!")
                );
    }


    @FXML
    private void handleGoRegister() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/view/register.fxml"));
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(loader.load(), 400, 420));
        } catch (Exception e) {
            errorLabel.setText("Loi: " + e.getMessage());
        }
    }
}