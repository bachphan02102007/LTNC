package util;

import java.util.Optional;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import model.User;
import network.SocketClient;
import util.Singleton.UserManager;

public class UiDialogs {

    public static void showProfileDialog() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return;

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Hồ sơ tài khoản");
        dialog.setHeaderText("Cập nhật thông tin cơ bản");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField username = new TextField(user.getUsername());
        username.setDisable(true);
        TextField role = new TextField(user.getRoleName());
        role.setDisable(true);
        TextField fullName = new TextField(user.getFullName());
        TextField phone = new TextField(user.getPhoneNumber());
        phone.setPromptText("Số điện thoại liên hệ");
        PasswordField newPassword = new PasswordField();
        newPassword.setPromptText("Để trống nếu không đổi");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(16));
        grid.addRow(0, new Label("Tên đăng nhập:"), username);
        grid.addRow(1, new Label("Vai trò:"), role);
        grid.addRow(2, new Label("Họ tên:"), fullName);
        grid.addRow(3, new Label("Số điện thoại:"), phone);
        grid.addRow(4, new Label("Mật khẩu mới:"), newPassword);
        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            user.setFullName(fullName.getText());
            user.setPhoneNumber(phone.getText());
            user.setPassword(newPassword.getText());
            DataStorage.saveUsers(UserManager.getInstance().getAllUsers());
            SocketClient client = SessionManager.getInstance().getSocketClient();
            if (client != null && client.isConnected()) {
                client.updateProfile(fullName.getText(), phone.getText(), newPassword.getText());
            }
            showInfo("Hồ sơ", "Đã cập nhật hồ sơ tài khoản.");
        }
    }

    public static void showWalletDialog() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return;
        if (!user.supportsWallet()) {
            showInfo("Ví đấu giá", "Chỉ tài khoản Bidder mới có ví tiền để đặt giá và thanh toán.");
            return;
        }

        Alert dialog = new Alert(Alert.AlertType.CONFIRMATION);
        dialog.setTitle("Ví đấu giá");
        dialog.setHeaderText("Số dư hiện tại: " + String.format("%,.0f VNĐ", user.getWalletBalance()));
        dialog.setContentText("Chọn thao tác với ví của bạn.");
        ButtonType deposit = new ButtonType("Nạp tiền");
        ButtonType withdraw = new ButtonType("Rút tiền");
        dialog.getButtonTypes().setAll(deposit, withdraw, ButtonType.CANCEL);

        Optional<ButtonType> choice = dialog.showAndWait();
        if (choice.isEmpty() || choice.get() == ButtonType.CANCEL) return;

        TextField amountField = new TextField();
        amountField.setPromptText("Nhập số tiền VNĐ");
        Dialog<ButtonType> amountDialog = new Dialog<>();
        amountDialog.setTitle(choice.get() == deposit ? "Nạp tiền" : "Rút tiền");
        amountDialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(16));
        grid.setHgap(12);
        grid.addRow(0, new Label("Số tiền:"), amountField);
        amountDialog.getDialogPane().setContent(grid);

        Optional<ButtonType> ok = amountDialog.showAndWait();
        if (ok.isEmpty() || ok.get() != ButtonType.OK) return;

        try {
            double amount = Double.parseDouble(amountField.getText().replace(",", "").trim());
            SocketClient client = SessionManager.getInstance().getSocketClient();
            if (client != null && client.isConnected()) {
                if (choice.get() == deposit) {
                    client.deposit(amount);
                } else {
                    client.withdraw(amount);
                }
                showInfo("Ví đấu giá", "Đã gửi thao tác " + (choice.get() == deposit ? "nạp" : "rút") + " tiền. Server sẽ cập nhật số dư ngay nếu hợp lệ.");
            } else {
                showError("Ví đấu giá", "Không kết nối server nên không thể thao tác ví.");
            }
        } catch (Exception e) {
            showError("Ví đấu giá", e.getMessage());
        }
    }

    public static void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public static void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
