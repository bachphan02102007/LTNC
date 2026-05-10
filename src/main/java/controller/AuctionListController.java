package controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import model.*;
import network.SocketClient;
import util.Factory.ItemFactory;
import util.SessionManager;
import util.Singleton.AuctionManager;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.ResourceBundle;

public class AuctionListController implements Initializable {

    @FXML private TableView<Auction> auctionTable;
    @FXML private TableColumn<Auction, String> colId;
    @FXML private TableColumn<Auction, String> colItem;
    @FXML private TableColumn<Auction, Double> colPrice;
    @FXML private TableColumn<Auction, String> colStatus;
    @FXML private TableColumn<Auction, String> colEnd;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colId.setCellValueFactory(new PropertyValueFactory<>("auctionId"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("currentHighestBid"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colItem.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getItem().getName()));
        colEnd.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getEndTime().toString()));

        loadSampleData();
        loadTable();
        connectSocketListener();
    }

    private void loadSampleData() {
        if (!AuctionManager.getInstance().getAllAuctions().isEmpty()) return;
        Item laptop   = ItemFactory.create("ELECTRONICS", "I001", "Laptop Dell", "Core i7", 500.0, "12");
        Item painting = ItemFactory.create("ART", "I002", "Tranh Son Dau", "Phong canh", 200.0, "Nguyen Van A");
        Auction a1 = new Auction("A001", laptop, LocalDateTime.now().plusMinutes(30));
        Auction a2 = new Auction("A002", painting, LocalDateTime.now().plusHours(2));
        a1.startAuction();
        AuctionManager.getInstance().addAuction(a1);
        AuctionManager.getInstance().addAuction(a2);
    }

    private void loadTable() {
        ObservableList<Auction> list = FXCollections.observableArrayList(
                AuctionManager.getInstance().getAllAuctions());
        auctionTable.setItems(list);
    }

    /**
     * Đăng ký callback lên SocketClient dùng chung từ SessionManager.
     * Khi Seller đăng sản phẩm → server broadcastAll "NEW_AUCTION:..."
     * → client này nhận và reload bảng tự động.
     */
    private void connectSocketListener() {
        SocketClient client = SessionManager.getInstance().getSocketClient();
        if (client == null || !client.isConnected()) return;

        client.setOnMessage(msg -> {
            if (msg.startsWith("NEW_AUCTION:")) {
                // Reload bảng trên JavaFX thread
                javafx.application.Platform.runLater(this::loadTable);
            } else if (msg.startsWith("GIA_MOI:")) {
                javafx.application.Platform.runLater(() -> auctionTable.refresh());
            }
        });
    }

    @FXML
    private void handleCreateAuction() {
        System.out.println("TODO: Mo form tao phien dau gia moi");
    }

    @FXML
    private void handleJoinAuction() {
        Auction selected = auctionTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Chua chon phien", "Vui long chon mot phien dau gia truoc khi tham gia.");
            return;
        }
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) {
            showAlert("Loi phien", "Phien dang nhap da het han. Vui long dang nhap lai.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/auction_room.fxml"));
            Stage stage = (Stage) auctionTable.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));
            AuctionRoomController roomCtrl = loader.getController();
            roomCtrl.initData(currentUser, selected.getAuctionId());
            stage.setTitle("Phong dau gia — " + selected.getAuctionId()
                    + " | " + selected.getItem().getName());
            stage.setOnCloseRequest(e -> roomCtrl.onClose());
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Loi", "Khong the mo man hinh dau gia:\n" + e.getMessage());
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
