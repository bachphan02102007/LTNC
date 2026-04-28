package view;

import javafx.application.Application;
import javafx.scene.Scene; //Scene = nội dung bên trong cửa sổ (căn phòng)
import javafx.scene.control.Label; //StackPane, Label... = đồ vật trong phòng (bàn, ghế...)
import javafx.scene.layout.StackPane;
import javafx.stage.Stage; //Stage = cái cửa sổ (khung nhà)

public class HelloApp extends Application { //Application là class gốc của JavaFX
                                        // — bắt buộc phải kế thừa để JavaFX biết đây là ứng dụng cần khởi động
    @Override
    public void start(Stage stage) {
        Label label = new Label("He thong dau gia truc tuyen");//Label là component hiển thị chữ
        label.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-font-family: 'Arial';");// điịnh dạng
        StackPane root = new StackPane(label);
        root.setStyle("-fx-background-color: #f0f4ff;");//xetsts màu nền

        Scene scene = new Scene(root, 500, 300);// kích cỡ
        stage.setTitle("Online Auction System"); // tên của app
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
// cách  thức hoạt động
//main() → launch() → [JavaFX khởi động] → start(stage) ->tạo Label -> bỏ vào StackPane
// -> bọc vào Scene -> gắn vào Stage -> stage.show() → cửa sổ hiện ra!