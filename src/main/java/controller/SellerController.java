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
import network.SocketClient;

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
        try {
            socketClient.connect(seller.getUsername(), this::handleServerMessage);
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
            buttonPost.setDisable(false);
        } else if (message.startsWith("ADD_ITEM_FAIL:")) {
            String reason = message.substring("ADD_ITEM_FAIL:".length());
            showResult("❌ Thất bại: " + reason, Color.RED);
            buttonPost.setDisable(false);
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
        listViewMyItems.getItems().add(0, name + " — " + startPrice + " VNĐ (" + duration + "s)");
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
    private void handleLogout(javafx.event.ActionEvent event) {
        try {
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
