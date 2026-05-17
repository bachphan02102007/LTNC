package controller;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.stage.Stage;
import model.Admin;
import model.Auction;
import model.User;
import util.DataStorage;
import util.SessionManager;
import util.Singleton.AuctionManager;
import util.Singleton.UserManager;
import util.UiDialogs;
import network.SocketClient;

public class AdminController implements Initializable {

    @FXML private Label labelStats;
    @FXML private ListView<String> listUsers;
    @FXML private ListView<String> listAuctions;
    private SocketClient socketClient;
    private Admin currentAdmin;

    public void initData(Admin admin, SocketClient existingClient) {
        this.currentAdmin = admin;
        this.socketClient = existingClient;
        try {
            socketClient.connect(admin.getUsername(), "ADMIN", this::handleServerMessage);
            reload();
        } catch (Exception e) {
            UiDialogs.showError("Admin", "Không thể kết nối server: " + e.getMessage());
        }
    }

    private void handleServerMessage(String message) {
        if (message.startsWith("DELETE_AUCTION_OK:")) {
            UiDialogs.showInfo("Admin", "Đã xóa phiên " + message.substring("DELETE_AUCTION_OK:".length()));
            reload();
        } else if (message.startsWith("DELETE_AUCTION_FAIL:")) {
            UiDialogs.showError("Admin", message.substring("DELETE_AUCTION_FAIL:".length()));
        } else if (message.startsWith("CANCEL_AUCTION_OK:")) {
            UiDialogs.showInfo("Admin", "Đã hủy phiên " + message.substring("CANCEL_AUCTION_OK:".length()));
            reload();
        } else if (message.startsWith("CANCEL_AUCTION_FAIL:")) {
            UiDialogs.showError("Admin", message.substring("CANCEL_AUCTION_FAIL:".length()));
        } else if (message.startsWith("LOCK_USER_OK:")) {
            UiDialogs.showInfo("Admin", "Đã khóa tài khoản " + message.substring("LOCK_USER_OK:".length()));
            reload();
        } else if (message.startsWith("UNLOCK_USER_OK:")) {
            UiDialogs.showInfo("Admin", "Đã mở lại tài khoản " + message.substring("UNLOCK_USER_OK:".length()));
            reload();
        } else if (message.startsWith("LOCK_USER_FAIL:")) {
            UiDialogs.showError("Admin", message.substring("LOCK_USER_FAIL:".length()));
        } else if (message.startsWith("UNLOCK_USER_FAIL:")) {
            UiDialogs.showError("Admin", message.substring("UNLOCK_USER_FAIL:".length()));
        } else if (message.startsWith("AUCTION_DELETED:")
                || message.startsWith("AUCTION_CANCELED:")
                || message.startsWith("NEW_AUCTION:")
                || message.startsWith("AUCTION_CLOSED:")
                || message.startsWith("AUCTION_PAID:")
                || message.startsWith("GIA_MOI:")
                || message.startsWith("USER_STATUS_CHANGED:")) {
            reload();
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        reload();
    }

    @FXML private void handleReload() { reload(); }

    @FXML
    private void handleCancelAuction() {
        String selected = listAuctions.getSelectionModel().getSelectedItem();
        if (selected == null) { showWarning("Hãy chọn một phiên đấu giá."); return; }
        String auctionId = selected.split("\\|")[0].trim();
        if (socketClient != null && socketClient.isConnected()) {
            socketClient.sendCommand("CANCEL_AUCTION:" + auctionId);
        } else {
            boolean ok = AuctionManager.getInstance().cancelAuction(auctionId);
            if (ok) {
                DataStorage.saveAuctions(AuctionManager.getInstance().getAllAuctions());
                reload();
                UiDialogs.showInfo("Admin", "Đã hủy phiên " + auctionId);
            }
        }
    }

    @FXML
    private void handleDeleteAuction() {
        String selected = listAuctions.getSelectionModel().getSelectedItem();
        if (selected == null) { showWarning("Hãy chọn một phiên đấu giá để xóa."); return; }
        String auctionId = selected.split("\\|")[0].trim();
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xóa phiên đấu giá");
        confirm.setHeaderText("Xóa vĩnh viễn phiên " + auctionId + " khỏi danh sách?");
        confirm.setContentText("Phiên này sẽ biến mất khỏi auction_list và dữ liệu auctions.dat.");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                if (socketClient != null && socketClient.isConnected()) {
                    socketClient.sendCommand("DELETE_AUCTION:" + auctionId);
                } else if (AuctionManager.getInstance().removeAuction(auctionId)) {
                    DataStorage.saveAuctions(AuctionManager.getInstance().getAllAuctions());
                    reload();
                }
            }
        });
    }

    @FXML
    private void handleDeactivateUser() {
        String username = selectedUsername();
        if (username == null) { showWarning("Hãy chọn một người dùng."); return; }
        User current = SessionManager.getInstance().getCurrentUser();
        if (current != null && current.getUsername().equals(username)) {
            showWarning("Không thể khóa chính tài khoản admin đang đăng nhập.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Khóa tài khoản");
        confirm.setHeaderText("Khóa tài khoản " + username + "?");
        confirm.setContentText("Tài khoản sẽ không đăng nhập được và server sẽ chặn thao tác mới.");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                if (socketClient != null && socketClient.isConnected()) {
                    socketClient.sendCommand("LOCK_USER:" + username);
                } else {
                    UserManager.getInstance().deactivateUser(username);
                    DataStorage.saveUsers(UserManager.getInstance().getAllUsers());
                    reload();
                }
            }
        });
    }

    @FXML
    private void handleActivateUser() {
        String username = selectedUsername();
        if (username == null) { showWarning("Hãy chọn một người dùng."); return; }
        if (socketClient != null && socketClient.isConnected()) {
            socketClient.sendCommand("UNLOCK_USER:" + username);
        } else if (UserManager.getInstance().activateUser(username)) {
            DataStorage.saveUsers(UserManager.getInstance().getAllUsers());
            reload();
            UiDialogs.showInfo("Admin", "Đã mở lại tài khoản " + username);
        }
    }

    @FXML private void handleProfile() { UiDialogs.showProfileDialog(); }

    @FXML
    private void handleLogout() {
        try {
            SessionManager.getInstance().logout();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/login.fxml"));
            Stage stage = (Stage) listUsers.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));
            stage.setTitle("Đăng nhập");
            stage.show();
        } catch (Exception e) {
            UiDialogs.showError("Lỗi", e.getMessage());
        }
    }

    private void reload() {
        var latestUsers = DataStorage.<User>loadUsers();
        if (!latestUsers.isEmpty()) UserManager.getInstance().loadFrom(latestUsers);
        var latestAuctions = DataStorage.<Auction>loadAuctions();
        AuctionManager.getInstance().clearAuctions();
        latestAuctions.forEach(AuctionManager.getInstance()::addAuction);

        var users = UserManager.getInstance().getAllUsers();
        var auctions = AuctionManager.getInstance().getAllAuctions();
        long running = auctions.stream().filter(a -> "RUNNING".equals(a.getStatus().name())).count();
        labelStats.setText("Người dùng: " + users.size() + "   |   Phiên đấu giá: " + auctions.size()
                + "   |   Đang chạy: " + running);

        listUsers.setItems(FXCollections.observableArrayList(users.stream()
                .map(u -> u.getUsername() + " | " + u.getRoleName()
                        + " | " + (u.isActive() ? "ACTIVE" : "LOCKED")
                        + " | SĐT: " + (u.getPhoneNumber().isBlank() ? "—" : u.getPhoneNumber())
                        + (u.supportsWallet() ? " | Ví: " + String.format("%,.0f", u.getWalletBalance()) + " VNĐ" : ""))
                .collect(Collectors.toList())));

        listAuctions.setItems(FXCollections.observableArrayList(auctions.stream()
                .map(this::formatAuction)
                .collect(Collectors.toList())));
    }

    private String formatAuction(Auction a) {
        return a.getAuctionId() + " | " + a.getItem().getName()
                + " | " + a.getStatus()
                + " | Giá: " + String.format("%,.0f", a.getCurrentHighestBid()) + " VNĐ"
                + " | Seller: " + (a.getSellerUsername() == null ? "—" : a.getSellerUsername())
                + " | Bids: " + a.getBidHistory().size();
    }

    private String selectedUsername() {
        String selected = listUsers.getSelectionModel().getSelectedItem();
        if (selected == null) return null;
        return selected.split("\\|")[0].trim();
    }

    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
