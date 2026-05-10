package controller;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import model.Auction;
import model.BidTransaction;
import model.User;
import network.SocketClient;
import util.SessionManager;
import util.Singleton.AuctionManager;

/**
 * Màn hình đấu giá realtime hoàn chỉnh:
 *  - Đồng hồ đếm ngược thời gian thực (Timeline JavaFX)
 *  - Bảng lịch sử bid tự cập nhật
 *  - Quick-bid buttons (+100k, +500k, +1tr)
 *  - Popup thông báo kết thúc phiên
 *  - Dùng chung SocketClient từ SessionManager
 */
public class AuctionRoomController implements Initializable {

    // ── Header ──
    @FXML private Label labelAuctionId;
    @FXML private Label labelItemName;
    @FXML private Label labelCountdown;
    @FXML private Label labelStatus;

    // ── Giá ──
    @FXML private Label labelCurrentBid;
    @FXML private Label labelLeader;

    // ── Form đặt giá ──
    @FXML private TextField fieldBidAmount;
    @FXML private Button    buttonBid;
    @FXML private Label     labelBidResult;

    // ── Lịch sử bid ──
    @FXML private TableView<BidTransaction>          tableBidHistory;
    @FXML private TableColumn<BidTransaction, String> colBidder;
    @FXML private TableColumn<BidTransaction, String> colAmount;
    @FXML private TableColumn<BidTransaction, String> colTime;
    @FXML private Label labelBidCount;

    // ── Item info ──
    @FXML private Label labelCategory;
    @FXML private Label labelStartPrice;

    // ── Log ──
    @FXML private ListView<String> listViewLog;

    // ── State ──
    private SocketClient socketClient;
    private User currentUser;
    private String currentAuctionId;
    private Auction currentAuction;
    private Timeline countdownTimeline;
    private final ObservableList<String>         logMessages  = FXCollections.observableArrayList();
    private final ObservableList<BidTransaction> bidHistory   = FXCollections.observableArrayList();
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        listViewLog.setItems(logMessages);

        // Cột lịch sử bid
        colBidder.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getBidder().getUsername()));
        colAmount.setCellValueFactory(c ->
                new SimpleStringProperty(
                        String.format("%,.0f VNĐ", c.getValue().getAmount())));
        colTime.setCellValueFactory(c ->
                new SimpleStringProperty(
                        c.getValue().getTimestamp().format(TIME_FMT)));
        tableBidHistory.setItems(bidHistory);

        buttonBid.setDisable(true);
    }

    /**
     * Gọi từ AuctionListController khi user chọn vào 1 phiên.
     */
    public void initData(User user, String auctionId) {
        this.currentUser      = user;
        this.currentAuctionId = auctionId;
        this.currentAuction   = AuctionManager.getInstance()
                .findById(auctionId).orElse(null);

        labelAuctionId.setText("Phiên: " + auctionId);

        if (currentAuction != null) {
            labelItemName.setText(currentAuction.getItem().getName());
            labelCurrentBid.setText(String.format("%,.0f VNĐ",
                    currentAuction.getCurrentHighestBid()));
            labelCategory.setText(
                    currentAuction.getItem().getClass().getSimpleName());
            labelStartPrice.setText(String.format("%,.0f VNĐ",
                    currentAuction.getItem().getStartingPrice()));

            // Load lịch sử bid hiện có
            bidHistory.setAll(currentAuction.getBidHistory());
            labelBidCount.setText(String.valueOf(bidHistory.size()));

            // Khởi động đồng hồ đếm ngược
            startCountdown(currentAuction.getEndTime());
        }

        connectToServer();
    }

    // ── Countdown ─────────────────────────────────────────────────────────────

    private void startCountdown(LocalDateTime endTime) {
        if (countdownTimeline != null) countdownTimeline.stop();

        countdownTimeline = new Timeline(new KeyFrame(
                javafx.util.Duration.seconds(1), e -> updateCountdown(endTime)));
        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();
        updateCountdown(endTime); // hiện ngay lập tức
    }

    private void updateCountdown(LocalDateTime endTime) {
        Duration remaining = Duration.between(LocalDateTime.now(), endTime);
        if (remaining.isNegative() || remaining.isZero()) {
            labelCountdown.setText("00:00");
            labelCountdown.setStyle(
                    "-fx-text-fill: #e74c3c; -fx-font-size: 22px; -fx-font-weight: bold;");
            if (countdownTimeline != null) countdownTimeline.stop();
            return;
        }

        long totalSeconds = remaining.getSeconds();
        long hours   = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        String display = hours > 0
                ? String.format("%02d:%02d:%02d", hours, minutes, seconds)
                : String.format("%02d:%02d", minutes, seconds);
        labelCountdown.setText(display);

        // Đổi màu khi còn < 30 giây
        if (totalSeconds <= 30) {
            labelCountdown.setStyle(
                    "-fx-text-fill: #e74c3c; -fx-font-size: 22px; -fx-font-weight: bold;");
        } else if (totalSeconds <= 60) {
            labelCountdown.setStyle(
                    "-fx-text-fill: #e67e22; -fx-font-size: 22px; -fx-font-weight: bold;");
        } else {
            labelCountdown.setStyle(
                    "-fx-text-fill: #f0c040; -fx-font-size: 22px; -fx-font-weight: bold;");
        }
    }

    // ── Socket ────────────────────────────────────────────────────────────────

    private void connectToServer() {
        // Dùng chung SocketClient từ SessionManager — không tạo kết nối mới
        socketClient = SessionManager.getInstance().getSocketClient();

        if (socketClient != null && socketClient.isConnected()) {
            socketClient.setOnMessage(this::handleServerMessage);
            setStatus("✅ Đã kết nối", Color.GREEN);
            buttonBid.setDisable(false);
            addLog("👋 Đã vào phòng đấu giá " + currentAuctionId);
        } else {
            // Fallback: tạo kết nối mới nếu SessionManager chưa có
            socketClient = new SocketClient();
            try {
                socketClient.connect(currentUser.getUsername(),
                        this::handleServerMessage);
                setStatus("✅ Đã kết nối", Color.GREEN);
                buttonBid.setDisable(false);
                addLog("👋 Đã vào phòng đấu giá " + currentAuctionId);
            } catch (IOException e) {
                setStatus("❌ Không thể kết nối server", Color.RED);
                addLog("Lỗi: " + e.getMessage());
            }
        }
    }

    // ── Message handler ───────────────────────────────────────────────────────

    private void handleServerMessage(String message) {
        // Không log các message hệ thống ít quan trọng
        if (!message.startsWith("ONLINE:") && !message.startsWith("LIST_OK:")) {
            addLog("[" + LocalDateTime.now().format(TIME_FMT) + "] " + message);
        }

        if (message.startsWith("GIA_MOI:")) {
            handleGiaMoi(message);
        } else if (message.startsWith("BID_OK:")) {
            handleBidOk(message);
        } else if (message.startsWith("BID_FAIL:")) {
            handleBidFail(message);
        } else if (message.startsWith("AUCTION_CLOSED:")) {
            handleAuctionClosed(message);
        } else if (message.startsWith("TIME_EXTENDED:")) {
            handleTimeExtended(message);
        } else if (message.startsWith("ERROR:")) {
            setStatus("❌ " + message.substring(6), Color.RED);
        }
    }

    /**
     * GIA_MOI:auctionId:amount:bidder
     * Cập nhật giá + reload lịch sử bid.
     */
    private void handleGiaMoi(String message) {
        String[] parts = message.split(":");
        if (parts.length < 4) return;
        if (!parts[1].equals(currentAuctionId)) return;

        double amount  = Double.parseDouble(parts[2]);
        String bidder  = parts[3];

        labelCurrentBid.setText(String.format("%,.0f VNĐ", amount));
        labelCurrentBid.setTextFill(Color.ORANGERED);
        labelLeader.setText("👑 Người dẫn đầu: " + bidder);

        // Reload lịch sử bid từ AuctionManager
        if (currentAuction != null) {
            bidHistory.setAll(currentAuction.getBidHistory());
            labelBidCount.setText(String.valueOf(bidHistory.size()));
        }

        addLog("🔔 " + bidder + " vừa đặt " + String.format("%,.0f VNĐ", amount));
    }

    /** BID_OK:amount */
    private void handleBidOk(String message) {
        double amount = Double.parseDouble(message.split(":")[1]);
        setBidResult("✅ Đặt giá " + String.format("%,.0f VNĐ", amount)
                + " thành công!", Color.GREEN);
        fieldBidAmount.clear();
        setStatus("✅ Đặt giá thành công", Color.GREEN);
    }

    /** BID_FAIL:reason */
    private void handleBidFail(String message) {
        String reason = message.substring("BID_FAIL:".length());
        setBidResult("❌ " + reason, Color.RED);
        setStatus("❌ Đặt giá thất bại", Color.RED);
    }

    /**
     * AUCTION_CLOSED:auctionId:winner:finalPrice
     * Dừng countdown, disable form, hiện popup kết quả.
     */
    private void handleAuctionClosed(String message) {
        String[] parts = message.split(":");
        if (parts.length < 4) return;
        if (!parts[1].equals(currentAuctionId)) return;

        String winner     = parts[2];
        String finalPrice = parts[3];

        if (countdownTimeline != null) countdownTimeline.stop();
        labelCountdown.setText("KẾT THÚC");
        buttonBid.setDisable(true);
        fieldBidAmount.setDisable(true);

        boolean iWon = winner.equals(currentUser.getUsername());
        setStatus("🏁 Phiên kết thúc!", Color.PURPLE);
        addLog("🏁 Phiên kết thúc! Người thắng: " + winner
                + " — " + String.format("%,.0f VNĐ",
                Double.parseDouble(finalPrice)));

        // Popup thông báo kết quả
        showResultDialog(winner, finalPrice, iWon);
    }

    /**
     * TIME_EXTENDED:auctionId:newEndTime
     * Anti-sniping: cập nhật countdown khi phiên được gia hạn.
     */
    private void handleTimeExtended(String message) {
        String[] parts = message.split(":", 3);
        if (parts.length < 3) return;
        if (!parts[1].equals(currentAuctionId)) return;

        LocalDateTime newEnd = LocalDateTime.parse(parts[2]);
        if (currentAuction != null) currentAuction.setEndTime(newEnd);
        startCountdown(newEnd);
        addLog("⏰ Phiên được gia hạn thêm thời gian!");
    }

    // ── Popup kết quả ─────────────────────────────────────────────────────────

    private void showResultDialog(String winner, String finalPrice, boolean iWon) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        if (iWon) {
            alert.setTitle("🎉 Chúc mừng!");
            alert.setHeaderText("Bạn đã thắng phiên đấu giá!");
            alert.setContentText("Sản phẩm: " + (currentAuction != null
                    ? currentAuction.getItem().getName() : "—")
                    + "\nGiá thắng: " + String.format("%,.0f VNĐ",
                    Double.parseDouble(finalPrice))
                    + "\n\nVui lòng liên hệ Seller để hoàn tất giao dịch.");
        } else if ("KHONG_AI".equals(winner) || winner.isEmpty()) {
            alert.setTitle("Phiên kết thúc");
            alert.setHeaderText("Không có người đặt giá");
            alert.setContentText("Phiên đấu giá kết thúc mà không có người tham gia.");
        } else {
            alert.setTitle("Phiên kết thúc");
            alert.setHeaderText("Người thắng: " + winner);
            alert.setContentText("Giá thắng: " + String.format("%,.0f VNĐ",
                    Double.parseDouble(finalPrice)));
        }
        alert.showAndWait();
    }

    // ── Đặt giá ──────────────────────────────────────────────────────────────

    @FXML
    private void handleBid() {
        String text = fieldBidAmount.getText().trim();
        if (text.isEmpty()) {
            setBidResult("⚠️ Vui lòng nhập số tiền", Color.ORANGE);
            return;
        }
        try {
            double amount = Double.parseDouble(text.replace(",", ""));
            if (amount <= 0) {
                setBidResult("⚠️ Số tiền phải > 0", Color.ORANGE);
                return;
            }
            double current = currentAuction != null
                    ? currentAuction.getCurrentHighestBid() : 0;
            if (amount <= current) {
                setBidResult(String.format("⚠️ Phải cao hơn %,.0f VNĐ", current),
                        Color.ORANGE);
                return;
            }
            socketClient.sendBid(currentAuctionId, amount);
            setBidResult("⏳ Đang gửi...", Color.GRAY);
        } catch (NumberFormatException e) {
            setBidResult("⚠️ Số tiền không hợp lệ", Color.RED);
        }
    }

    /** +100,000 VNĐ so với giá hiện tại */
    @FXML private void handleQuickBid100()  { quickBid(100_000); }
    /** +500,000 VNĐ so với giá hiện tại */
    @FXML private void handleQuickBid500()  { quickBid(500_000); }
    /** +1,000,000 VNĐ so với giá hiện tại */
    @FXML private void handleQuickBid1000() { quickBid(1_000_000); }

    private void quickBid(double increment) {
        double current = currentAuction != null
                ? currentAuction.getCurrentHighestBid() : 0;
        double suggest = current + increment;
        fieldBidAmount.setText(String.format("%.0f", suggest));
        fieldBidAmount.requestFocus();
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    @FXML
    private void handleBack() {
        if (countdownTimeline != null) countdownTimeline.stop();
        // Không disconnect socket — dùng chung với SessionManager
        // Chỉ reset callback về null
        if (socketClient != null) socketClient.setOnMessage(null);
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/view/auction_list.fxml"));
            Stage stage = (Stage) buttonBid.getScene().getWindow();
            stage.setScene(new Scene(loader.load(), 700, 500));
            stage.setTitle("Danh sach Phien Dau Gia");
        } catch (IOException e) {
            addLog("Lỗi quay lại: " + e.getMessage());
        }
    }

    /** Gọi từ setOnCloseRequest khi đóng cửa sổ */
    public void onClose() {
        if (countdownTimeline != null) countdownTimeline.stop();
        // Chỉ disconnect nếu không dùng chung SessionManager
        SocketClient session = SessionManager.getInstance().getSocketClient();
        if (socketClient != null && socketClient != session) {
            socketClient.disconnect();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void addLog(String message) {
        logMessages.add(0, message);
        if (logMessages.size() > 150) logMessages.remove(logMessages.size() - 1);
    }

    private void setStatus(String text, Color color) {
        labelStatus.setText(text);
        labelStatus.setTextFill(color);
    }

    private void setBidResult(String text, Color color) {
        labelBidResult.setText(text);
        labelBidResult.setTextFill(color);
    }
}
