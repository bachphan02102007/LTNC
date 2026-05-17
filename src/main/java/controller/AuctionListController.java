package controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import model.*;
import network.SocketClient;
import util.Factory.ItemFactory;
import util.SessionManager;
import util.UiDialogs;
import util.Singleton.AuctionManager;
import javafx.event.ActionEvent;
import javafx.stage.Window;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.ResourceBundle;

public class AuctionListController implements Initializable {

    @FXML private TilePane auctionContainer;

    @FXML
    private Label labelEmpty;

    @FXML
    private Button buttonLogout;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadCards();
        connectSocketListener();

        SocketClient client = SessionManager.getInstance().getSocketClient();
        if (client != null && client.isConnected()) {
            client.requestList();
        }
    }

    private void loadSampleData() {
        if (!AuctionManager.getInstance().getAllAuctions().isEmpty()) return;

        Item laptop = ItemFactory.create("ELECTRONICS", "I001", "Laptop Dell", "Core i7", 500.0, "12");
        Item painting = ItemFactory.create("ART", "I002", "Tranh Sơn Dầu", "Phong cảnh", 200.0, "Nguyễn Văn A");

        Auction a1 = new Auction("A001", laptop, LocalDateTime.now().plusMinutes(30));
        Auction a2 = new Auction("A002", painting, LocalDateTime.now().plusHours(2));

        a1.startAuction();

        AuctionManager.getInstance().addAuction(a1);
        AuctionManager.getInstance().addAuction(a2);
    }

    private void loadCards() {

        auctionContainer.getChildren().clear();

        var auctions = AuctionManager.getInstance()
                .getAllAuctions()
                .stream()
                .filter(a -> a.getEndTime()
                        .plusDays(1)
                        .isAfter(LocalDateTime.now()))
                .sorted((a, b) -> b.getStartTime().compareTo(a.getStartTime()))
                .toList();        if (auctions.isEmpty()) {

            labelEmpty.setVisible(true);

            return;
        }

        // Có dữ liệu
        labelEmpty.setVisible(false);

        for (Auction auction : auctions) {
            auctionContainer.getChildren().add(createAuctionCard(auction));
        }
    }

    private VBox createAuctionCard(Auction auction) {
        VBox card = new VBox(14);
        card.setPrefWidth(260);
        card.setPadding(new Insets(20));
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 24;" +
                        "-fx-border-radius: 24;" +
                        "-fx-border-color: #dbeafe;" +
                        "-fx-effect: dropshadow(gaussian, rgba(37,99,235,0.18), 18, 0, 0, 8);"
        );

        Label tag = new Label(auction.getStatus().name());
        tag.setStyle(
                "-fx-background-color: #dbeafe;" +
                        "-fx-text-fill: #2563eb;" +
                        "-fx-font-size: 12px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 6 12;" +
                        "-fx-background-radius: 999;"
        );

        Label name = new Label(auction.getItem().getName());
        name.setWrapText(true);
        name.setStyle("-fx-text-fill: #0f172a; -fx-font-size: 21px; -fx-font-weight: bold;");

        Label id = new Label("Mã phiên: " + auction.getAuctionId());
        id.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px;");

        Label priceTitle = new Label("Giá hiện tại");
        priceTitle.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px;");

        Label price = new Label(String.format("%.0f VNĐ", auction.getCurrentHighestBid()));
        price.setStyle("-fx-text-fill: #0284c7; -fx-font-size: 24px; -fx-font-weight: bold;");
        Label seller = new Label("Seller: " + (auction.getSellerUsername() == null || auction.getSellerUsername().isBlank() ? "—" : auction.getSellerUsername()));
        seller.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");

        Label end = new Label("Kết thúc: " + auction.getEndTime());
        end.setWrapText(true);
        end.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");

        Button joinBtn = new Button("Tham gia đấu giá");
        joinBtn.setMaxWidth(Double.MAX_VALUE);
        joinBtn.setStyle(
                "-fx-background-color: linear-gradient(to right, #38bdf8, #2563eb);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 12;" +
                        "-fx-background-radius: 16;" +
                        "-fx-cursor: hand;"
        );

        joinBtn.setOnAction(e -> openAuctionRoom(auction));

        card.getChildren().addAll(tag, name, id, priceTitle, price, seller, end, joinBtn);
        return card;
    }

    private void connectSocketListener() {
        SocketClient client = SessionManager.getInstance().getSocketClient();

        if (client == null || !client.isConnected()) {
            System.out.println("[AuctionList] Socket chua ket noi");
            return;
        }

        client.setOnMessage(msg -> {
            System.out.println("[AuctionList nhan] " + msg);

            try {
                if (msg.startsWith("NEW_AUCTION:")) {
                    System.out.println("[AuctionList] Co phien moi -> request LIST");
                    client.requestList();
                }

                else if (msg.startsWith("LIST_OK:")) {

                    String data = msg.substring("LIST_OK:".length());

                    AuctionManager.getInstance().clearAuctions();

                    if (data.equals("Chua co phien nao") || data.isBlank()) {
                        loadCards();
                        return;
                    }

                    String[] auctions = data.split(",");

                    for (String a : auctions) {
                        String[] p = a.split("\\|");

                        if (p.length < 5) continue;

                        String id = p[0];
                        String name = p[1];
                        double price = Double.parseDouble(p[2]);
                        String category = p[3];
                        LocalDateTime endTime = LocalDateTime.parse(p[4]);
                        String sellerName = p.length > 6 ? p[6] : "";

                        Item item = ItemFactory.create(
                                category.toUpperCase(),
                                "ITEM-" + System.nanoTime(),
                                name,
                                "",
                                price,
                                "0"
                        );

                        Auction auction = new Auction(id, item, endTime, sellerName);
                        String statusName = p.length > 5 ? p[5] : "OPEN";
                        String leaderName = p.length > 7 ? p[7] : "";
                        try {
                            auction.restoreSnapshot(price, leaderName, AuctionStatus.valueOf(statusName), java.util.List.of());
                        } catch (Exception ignored) {
                            auction.restoreSnapshot(price, leaderName, AuctionStatus.OPEN, java.util.List.of());
                        }

                        AuctionManager.getInstance().addAuction(auction);
                    }

                    loadCards();
                }

                else if (msg.startsWith("GIA_MOI:")) {
                    client.requestList();
                }

                else if (msg.startsWith("WALLET_OK:")) {
                    showAlert("Ví đấu giá", msg.substring("WALLET_OK:".length()));
                    client.requestWallet();
                }

                else if (msg.startsWith("WALLET_FAIL:")) {
                    showAlert("Ví đấu giá", msg.substring("WALLET_FAIL:".length()));
                }

                else if (msg.startsWith("WALLET_INFO:")) {
                    User user = SessionManager.getInstance().getCurrentUser();
                    if (user != null) user.setWalletBalance(Double.parseDouble(msg.substring("WALLET_INFO:".length())));
                }

                else if (msg.startsWith("WALLET_CHANGED:")) {
                    User user = SessionManager.getInstance().getCurrentUser();
                    String[] p = msg.split(":", -1);
                    if (user != null && p.length > 2 && user.getUsername().equals(p[1])) {
                        user.setWalletBalance(Double.parseDouble(p[2]));
                        showAlert("Ví đấu giá", "Số dư ví đã cập nhật: " + String.format("%,.0f VNĐ", user.getWalletBalance()));
                    }
                }

                else if (msg.startsWith("MY_BIDDER_AUCTIONS_OK:")) {
                    showMyBidderAuctions(msg.substring("MY_BIDDER_AUCTIONS_OK:".length()));
                }

                else if (msg.startsWith("MY_BIDDER_AUCTIONS_FAIL:")) {
                    showAlert("Lịch sử đấu giá", msg.substring("MY_BIDDER_AUCTIONS_FAIL:".length()));
                }

                else if (msg.startsWith("AUCTION_DELETED:")
                        || msg.startsWith("AUCTION_CANCELED:")
                        || msg.startsWith("AUCTION_CLOSED:")
                        || msg.startsWith("AUCTION_PAID:")) {
                    client.requestList();
                }

                else if (msg.startsWith("USER_STATUS_CHANGED:")) {
                    String[] p = msg.split(":", -1);
                    User user = SessionManager.getInstance().getCurrentUser();
                    if (user != null && p.length > 2 && user.getUsername().equals(p[1]) && "LOCKED".equals(p[2])) {
                        showAlert("Tài khoản", "Tài khoản của bạn đã bị Admin khóa. Bạn sẽ không thể thao tác thêm.");
                    }
                    client.requestList();
                }

                else if (msg.startsWith("PROFILE_OK:")) {
                    User user = SessionManager.getInstance().getCurrentUser();
                    if (user != null) {
                        String[] p = msg.substring("PROFILE_OK:".length()).split(":", -1);
                        user.setFullName(p.length > 0 ? p[0] : "");
                        if (p.length > 1) user.setPhoneNumber(p[1]);
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }



    private void openAuctionRoom(Auction selected) {
        User currentUser = SessionManager.getInstance().getCurrentUser();

        if (currentUser == null) {
            showAlert("Lỗi phiên", "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/auction_room.fxml"));
            Stage stage = (Stage) auctionContainer.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));

            AuctionRoomController roomCtrl = loader.getController();
            roomCtrl.initData(currentUser, selected.getAuctionId());

            stage.setTitle("Phòng đấu giá — " + selected.getAuctionId());
            stage.setOnCloseRequest(e -> roomCtrl.onClose());
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Lỗi", "Không thể mở màn hình đấu giá:\n" + e.getMessage());
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    private void handleMyBidderAuctions() {
        SocketClient client = SessionManager.getInstance().getSocketClient();
        if (client != null && client.isConnected()) {
            client.sendCommand("MY_BIDDER_AUCTIONS");
        } else {
            showAlert("Lịch sử đấu giá", "Chưa kết nối server.");
        }
    }

    private void showMyBidderAuctions(String payload) {
        String[] sections = payload.split("::", -1);
        String joined = sections.length > 0 ? sections[0] : "EMPTY";
        String won = sections.length > 1 ? sections[1] : "EMPTY";
        StringBuilder sb = new StringBuilder();
        sb.append("CÁC PHIÊN ĐÃ THAM GIA:\n");
        appendAuctionRows(sb, joined);
        sb.append("\nCÁC PHIÊN ĐÃ THẮNG:\n");
        appendAuctionRows(sb, won);
        UiDialogs.showInfo("Phiên của tôi", sb.toString());
    }

    private void appendAuctionRows(StringBuilder sb, String data) {
        if (data == null || data.isBlank() || data.equals("EMPTY")) {
            sb.append("• Chưa có dữ liệu.\n");
            return;
        }
        for (String row : data.split(",")) {
            String[] p = row.split("\\|", -1);
            if (p.length < 6) continue;
            sb.append("• ").append(p[0]).append(" — ").append(p[1])
              .append(" | Giá: ").append(String.format("%,.0f", Double.parseDouble(p[2]))).append(" VNĐ")
              .append(" | Trạng thái: ").append(p[5])
              .append(p.length > 6 && !p[6].isBlank() ? " | Seller: " + p[6] : "")
              .append(p.length > 7 && !p[7].isBlank() ? " | Leader/Winner: " + p[7] : "")
              .append("\n");
        }
    }

    @FXML
    private void handleProfile() {
        UiDialogs.showProfileDialog();
    }

    @FXML
    private void handleWallet() {
        UiDialogs.showWalletDialog();
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            MenuItem menuItem = (MenuItem) event.getSource();

            Window window = menuItem.getParentPopup().getOwnerWindow();
            Stage stage = (Stage) window;

            SessionManager.getInstance().logout();

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/view/login.fxml"));

            Scene scene = new Scene(loader.load());

            stage.setScene(scene);
            stage.setTitle("Đăng nhập");
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Lỗi");
            alert.setHeaderText("Không thể quay về màn hình đăng nhập.");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }
}