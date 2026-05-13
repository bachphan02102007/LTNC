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

        var auctions = AuctionManager.getInstance().getRunningAuctions();        // Không có dữ liệu
        if (auctions.isEmpty()) {

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

        Label tag = new Label();
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

        card.getChildren().addAll(tag, name, id, priceTitle, price, end, joinBtn);
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

                    String[] parts = msg.split(":");

                    String auctionId = parts[1];
                    String itemName = parts[2];
                    double startPrice = Double.parseDouble(parts[3]);
                    String category = parts[4].toUpperCase();

                    String extraInfo = "0";
                    if (category.equals("ART")) {
                        extraInfo = "Unknown Artist";
                    } else if (category.equals("VEHICLE")) {
                        extraInfo = "Unknown Model";
                    }

                    Item item = ItemFactory.create(
                            category,
                            "ITEM-" + System.currentTimeMillis(),
                            itemName,
                            "",
                            startPrice,
                            extraInfo
                    );

                    Auction auction = new Auction(
                            auctionId,
                            item,
                            java.time.LocalDateTime.now().plusHours(1)
                    );

                    auction.startAuction();

                    AuctionManager.getInstance().addAuction(auction);

                    loadCards();
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

                        if (p.length < 3) continue;

                        String id = p[0];
                        String name = p[1];
                        double price = Double.parseDouble(p[2]);

                        Item item = ItemFactory.create(
                                "ELECTRONICS",
                                "ITEM-" + System.nanoTime(),
                                name,
                                "",
                                price,
                                "0"
                        );

                        Auction auction = new Auction(
                                id,
                                item,
                                java.time.LocalDateTime.now().plusHours(1)
                        );

                        auction.startAuction();

                        AuctionManager.getInstance().addAuction(auction);
                    }

                    loadCards();
                }

                else if (msg.startsWith("GIA_MOI:")) {
                    loadCards();
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