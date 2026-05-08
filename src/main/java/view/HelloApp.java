package view;

import exception.AuthenticationException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import model.Auction;
import model.User;
import util.Singleton.AuctionManager;
import util.DataStorage;
import util.Singleton.UserManager;

import java.util.List;

public class HelloApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        // ====== Load dữ liệu từ file khi app khởi động ======
        List<User> savedUsers = DataStorage.loadUsers();
        savedUsers.forEach(u -> {
            try {
                UserManager.getInstance().addUser(u);
            } catch (AuthenticationException ignored) {}
        });
        System.out.println("Da tai " + savedUsers.size() + " users tu file.");

        List<Auction> savedAuctions = DataStorage.loadAuctions();
        savedAuctions.forEach(a ->
                AuctionManager.getInstance().addAuction(a));
        System.out.println("Da tai " + savedAuctions.size() + " phien tu file.");

        // ====== Mở màn hình Login ======
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/view/login.fxml"));
        Scene scene = new Scene(loader.load(), 400, 350);
        stage.setTitle("Online Auction System");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}