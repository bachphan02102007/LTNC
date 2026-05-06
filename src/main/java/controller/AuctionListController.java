package controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import model.*;
import util.Singleton.AuctionManager;
import util.Factory.ItemFactory;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.ResourceBundle;

public class  AuctionListController implements Initializable {

    @FXML private TableView<Auction> auctionTable;
    @FXML private TableColumn<Auction, String> colId;
    @FXML private TableColumn<Auction, String> colItem;
    @FXML private TableColumn<Auction, Double> colPrice;
    @FXML private TableColumn<Auction, String> colStatus;
    @FXML private TableColumn<Auction, String> colEnd;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Gắn từng cột với thuộc tính tương ứng trong class Auction
        colId.setCellValueFactory(new PropertyValueFactory<>("auctionId"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("currentHighestBid"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Cột tên sản phẩm và thời gian cần xử lý thủ công
        colItem.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getItem().getName()));

        colEnd.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getEndTime().toString()));

        // Load dữ liệu giả để test giao diện
        loadSampleData();
        loadTable();
    }

    private void loadSampleData() {
        // Tạo vài phiên mẫu để nhìn thấy trên bảng
        Item laptop = ItemFactory.create("ELECTRONICS","I001","Laptop Dell","Core i7",500.0,"12");
        Item painting = ItemFactory.create("ART","I002","Tranh Son Dau","Phong canh",200.0,"Nguyen Van A");

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

    @FXML
    private void handleCreateAuction() {

        // TODO tuần 8: mở form tạo phiên mới

        System.out.println("TODO: Mo form tao phien dau gia moi");
    }

    @FXML
    private void handleJoinAuction() {
        Auction selected = auctionTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            System.out.println("Chua chon phien nao!");
            return;
        }
        // TODO tuần 10: chuyển sang màn hình đấu giá realtime
        System.out.println("TODO: Chuyen sang man hinh dau gia: " + selected.getAuctionId());
    }
}