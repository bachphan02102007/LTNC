package controller;

import exception.AuthenticationException;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import model.*;
import util.DataStorage;
import util.Singleton.UserManager;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.UUID;

public class RegisterController implements Initializable {

    @FXML private TextField     usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmField;
    @FXML private ComboBox<String> roleCombo;
    @FXML private Label         errorLabel;
    @FXML private Label         successLabel;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        roleCombo.setItems(FXCollections.observableArrayList("Bidder", "Seller"));
        roleCombo.getSelectionModel().selectFirst();
    }

    @FXML
    private void handleRegister() {
        errorLabel.setText("");
        successLabel.setText("");

        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        String confirm  = confirmField.getText().trim();
        String role     = roleCombo.getValue();

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Vui long nhap day du thong tin!"); return;
        }
        if (!password.equals(confirm)) {
            errorLabel.setText("Mat khau xac nhan khong khop!"); return;
        }
        if (username.length() < 3) {
            errorLabel.setText("Ten dang nhap phai co it nhat 3 ky tu!"); return;
        }

        String id = UUID.randomUUID().toString().substring(0, 8);
        User newUser = role.equals("Seller")
                ? new Seller(id, username, password)
                : new Bidder(id, username, password);

        try {
            UserManager.getInstance().addUser(newUser);
            DataStorage.saveUsers(UserManager.getInstance().getAllUsers());
            successLabel.setText("Dang ky thanh cong! Dang chuyen ve dang nhap...");

        } catch (AuthenticationException e) {
            errorLabel.setText(e.getMessage());
        }
    }

    @FXML
    private void handleGoLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/view/login.fxml"));
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(loader.load(), 400, 350));
            stage.setTitle("Dang nhap");
        } catch (Exception e) {
            errorLabel.setText("Loi: " + e.getMessage());
        }
    }
}
