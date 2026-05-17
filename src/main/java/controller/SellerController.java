package controller;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import model.Seller;
import model.User;
import network.SocketClient;
import util.SessionManager;
import util.UiDialogs;
import util.Singleton.AuctionManager;

public class SellerController implements Initializable {

    @FXML private TextField fieldItemName;
    @FXML private TextField fieldStartPrice;
    @FXML private TextField fieldDuration;
    @FXML private ComboBox<String> comboCategory;
    @FXML private Button buttonPost;
    @FXML private Label labelResult;
    @FXML private ListView<String> listViewMyItems;

    private SocketClient socketClient;
    private Seller currentSeller;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        comboCategory.getItems().addAll("Electronics", "Art", "Vehicle");
        comboCategory.setValue("Electronics");
    }

    /**
     * Gọi từ LoginController sau khi Seller đăng nhập thành công.
     */
    public void initData(Seller seller, SocketClient existingClient) {
        this.currentSeller = seller;
        this.socketClient = existingClient;

        // Chỉ connect 1 lần duy nhất
        refreshMyItems();
        try {
            socketClient.connect(seller.getUsername(), "SELLER", this::handleServerMessage);
            socketClient.requestMyAuctions();
        } catch (Exception e) {
            showResult("❌ Không thể kết nối server: " + e.getMessage(), Color.RED);
            buttonPost.setDisable(true);
        }
    }

    /** Xử lý phản hồi từ server */
    private void handleServerMessage(String message) {
        if (message.startsWith("ADD_ITEM_OK:")) {
            String auctionId = message.split(":")[1];
            showResult("✅ Đã đăng thành công! Mã phiên: " + auctionId, Color.GREEN);
            socketClient.requestMyAuctions();
            buttonPost.setDisable(false);
        } else if (message.startsWith("ADD_ITEM_FAIL:")) {
            String reason = message.substring("ADD_ITEM_FAIL:".length());
            showResult("❌ Thất bại: " + reason, Color.RED);
            buttonPost.setDisable(false);
        } else if (message.startsWith("MY_AUCTIONS_OK:")) {
            updateMyAuctionList(message.substring("MY_AUCTIONS_OK:".length()));
        } else if (message.startsWith("MY_AUCTIONS_FAIL:")) {
            showResult("❌ " + message.substring("MY_AUCTIONS_FAIL:".length()), Color.RED);
        } else if (message.startsWith("DETAIL_OK:")) {
            showAuctionDetail(message.substring("DETAIL_OK:".length()));
        } else if (message.startsWith("DELETE_AUCTION_OK:")) {
            showResult("✅ Đã xóa phiên " + message.substring("DELETE_AUCTION_OK:".length()), Color.GREEN);
            socketClient.requestMyAuctions();
        } else if (message.startsWith("DELETE_AUCTION_FAIL:")) {
            showResult("❌ " + message.substring("DELETE_AUCTION_FAIL:".length()), Color.RED);
        } else if (message.startsWith("AUCTION_DELETED:")
                || message.startsWith("AUCTION_CANCELED:")
                || message.startsWith("AUCTION_CLOSED:")
                || message.startsWith("AUCTION_PAID:")
                || message.startsWith("GIA_MOI:")
                || message.startsWith("NEW_AUCTION:")) {
            socketClient.requestMyAuctions();
        } else if (message.startsWith("PROFILE_OK:")) {
            User user = SessionManager.getInstance().getCurrentUser();
            if (user != null) {
                String[] p = message.substring("PROFILE_OK:".length()).split(":", -1);
                user.setFullName(p.length > 0 ? p[0] : "");
                if (p.length > 1) user.setPhoneNumber(p[1]);
            }
        } else if (message.startsWith("WALLET_HIDDEN:")) {
            // Seller không dùng ví, bỏ qua thông báo hệ thống này.
        }
    }

    @FXML
    private void handlePostItem() {
        String name = fieldItemName.getText().trim();
        String priceText = fieldStartPrice.getText().trim();
        String durationText = fieldDuration.getText().trim();
        String category = comboCategory.getValue();

        if (name.isEmpty() || priceText.isEmpty() || durationText.isEmpty()) {
            showResult("⚠️ Vui lòng điền đầy đủ thông tin", Color.ORANGE);
            return;
        }

        double startPrice;
        int duration;
        try {
            startPrice = Double.parseDouble(priceText);
            duration = Integer.parseInt(durationText);
            if (startPrice <= 0 || duration <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showResult("⚠️ Giá khởi điểm và thời gian phải là số dương", Color.RED);
            return;
        }

        if (socketClient == null || !socketClient.isConnected()) {
            showResult("❌ Chưa kết nối server", Color.RED);
            return;
        }

        socketClient.sendAddItem(name, startPrice, duration, category);
        showResult("⏳ Đang gửi lên server...", Color.GRAY);
        buttonPost.setDisable(true); // tránh spam
        // Không thêm tạm vào list khi server chưa xác nhận.
        // List chỉ refresh sau ADD_ITEM_OK để tránh hiện sản phẩm ảo khi thất bại.
        clearForm();
    }

    @FXML
    private void handleGoAuctionList() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/view/auction_list.fxml"));
            Stage stage = (Stage) fieldItemName.getScene().getWindow();
            stage.setScene(new Scene(loader.load(), 700, 500));
            stage.setTitle("Danh sach Phien Dau Gia");
        } catch (Exception e) {
            showResult("Loi: " + e.getMessage(), Color.RED);
        }
    }

    private void updateMyAuctionList(String data) {
        if (listViewMyItems == null) return;
        if (data == null || data.isBlank() || data.equals("EMPTY")) {
            listViewMyItems.getItems().setAll("Chưa có sản phẩm nào được đăng.");
            return;
        }
        listViewMyItems.getItems().setAll(java.util.Arrays.stream(data.split(","))
                .map(row -> {
                    String[] p = row.split("\\|", -1);
                    if (p.length < 6) return row;
                    String leader = p.length > 7 && !p[7].isBlank() ? " | Leader: " + p[7] : "";
                    String bids = p.length > 8 ? " | Bids: " + p[8] : "";
                    return p[0] + " — " + p[1]
                            + " — Giá: " + String.format("%,.0f VNĐ", Double.parseDouble(p[2]))
                            + " — " + p[5] + leader + bids;
                })
                .toList());
    }


    @FXML
    private void handleViewSelectedAuction() {
        String id = selectedAuctionId();
        if (id == null) {
            showResult("⚠️ Hãy chọn một phiên đã đăng", Color.ORANGE);
            return;
        }
        socketClient.requestDetail(id);
    }

    @FXML
    private void handleDeleteSelectedAuction() {
        String id = selectedAuctionId();
        if (id == null) {
            showResult("⚠️ Hãy chọn một phiên để xóa", Color.ORANGE);
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xóa sản phẩm/phiên");
        confirm.setHeaderText("Xóa phiên " + id + " khỏi hệ thống?");
        confirm.setContentText("Sau khi xóa, phiên sẽ biến mất khỏi auction_list và danh sách của Seller.");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) socketClient.sendCommand("DELETE_AUCTION:" + id);
        });
    }

    private String selectedAuctionId() {
        String selected = listViewMyItems == null ? null : listViewMyItems.getSelectionModel().getSelectedItem();
        if (selected == null || selected.startsWith("Chưa có")) return null;
        return selected.split(" ")[0].trim();
    }

    private void showAuctionDetail(String payload) {
        String[] p = payload.split("\\|", -1);
        if (p.length < 10) return;
        StringBuilder sb = new StringBuilder();
        sb.append("Mã phiên: ").append(p[0]).append("\n");
        sb.append("Sản phẩm: ").append(p[1]).append("\n");
        sb.append("Giá hiện tại: ").append(String.format("%,.0f", Double.parseDouble(p[2]))).append(" VNĐ\n");
        sb.append("Trạng thái: ").append(p[6]).append("\n");
        sb.append("Seller: ").append(p[7]).append(" - SĐT: ").append(p[8].isBlank() ? "—" : p[8]).append("\n");
        sb.append("Người thắng/leader: ").append(p[9].isBlank() ? "—" : p[9]).append("\n\n");
        sb.append("Lịch sử người đã đặt:\n");
        if (p.length > 10 && !p[10].isBlank()) {
            for (String row : p[10].split(";")) {
                String[] b = row.split("#", -1);
                if (b.length >= 3) {
                    sb.append("• ").append(b[0])
                      .append(b.length > 3 && !b[3].isBlank() ? " | SĐT: " + b[3] : "")
                      .append(" | ").append(String.format("%,.0f", Double.parseDouble(b[1]))).append(" VNĐ")
                      .append(" | ").append(b[2]).append("\n");
                }
            }
        } else {
            sb.append("Chưa có ai đặt giá.\n");
        }
        UiDialogs.showInfo("Chi tiết phiên của Seller", sb.toString());
    }

    private void refreshMyItems() {
        if (currentSeller == null || listViewMyItems == null) return;
        listViewMyItems.getItems().setAll(
                AuctionManager.getInstance().findBySeller(currentSeller.getUsername())
                        .stream()
                        .map(a -> a.getAuctionId() + " — " + a.getItem().getName()
                                + " — " + String.format("%,.0f VNĐ", a.getCurrentHighestBid())
                                + " — " + a.getStatus())
                        .toList()
        );
    }

    private void showResult(String message, Color color) {
        labelResult.setText(message);
        labelResult.setTextFill(color);
    }

    private void clearForm() {
        fieldItemName.clear();
        fieldStartPrice.clear();
        fieldDuration.clear();
        comboCategory.setValue("Electronics");
    }
    @FXML
    private void handleProfile() {
        UiDialogs.showProfileDialog();
    }

    @FXML
    private void handleWallet() {
        UiDialogs.showInfo("Ví đấu giá", "Seller không cần ví tiền. Ví chỉ dành cho Bidder để đặt giá và thanh toán.");
    }

    @FXML
    private void handleLogout(javafx.event.ActionEvent event) {
        try {
            SessionManager.getInstance().logout();

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/view/login.fxml"));

            Stage stage = (Stage) buttonPost.getScene().getWindow();

            Scene scene = new Scene(loader.load());

            stage.setScene(scene);
            stage.setTitle("Đăng nhập");
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
