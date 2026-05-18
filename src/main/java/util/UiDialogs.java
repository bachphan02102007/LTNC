package util;

import java.util.Optional;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import model.User;
import network.SocketClient;
import util.Singleton.UserManager;

public class UiDialogs {
    private static final String BLUE = "#0284c7";
    private static final String DARK = "#0f172a";
    private static final String MUTED = "#64748b";
    private static final String BORDER = "#bfdbfe";
    private static final String SOFT = "#f8fbff";

    private static void styleDialog(DialogPane pane) {
        pane.setStyle("-fx-background-color: linear-gradient(to bottom right, #eff6ff, #ffffff);"
                + "-fx-border-color: #dbeafe; -fx-border-width: 1; -fx-background-radius: 24; -fx-border-radius: 24;");
        pane.lookupAll(".button").forEach(n -> n.setStyle("-fx-background-color: linear-gradient(to right, #38bdf8, #2563eb);"
                + "-fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 14; -fx-padding: 9 16; -fx-cursor: hand;"));
    }

    private static Label title(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: " + DARK + "; -fx-font-size: 22px; -fx-font-weight: bold;");
        return l;
    }

    private static Label hint(String text) {
        Label l = new Label(text);
        l.setWrapText(true);
        l.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 13px;");
        return l;
    }

    private static Label chip(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-background-color: #dbeafe; -fx-text-fill: #2563eb; -fx-font-weight: bold;"
                + "-fx-font-size: 12px; -fx-padding: 6 12; -fx-background-radius: 999;");
        return l;
    }

    private static TextField field(String value, boolean disabled) {
        TextField tf = new TextField(value == null ? "" : value);
        tf.setDisable(disabled);
        tf.setStyle("-fx-pref-height: 42; -fx-background-color: white; -fx-background-radius: 14;"
                + "-fx-border-radius: 14; -fx-border-color: " + BORDER + "; -fx-padding: 0 12;");
        return tf;
    }

    private static VBox card(Node... children) {
        VBox box = new VBox(10, children);
        box.setPadding(new Insets(16));
        box.setStyle("-fx-background-color: white; -fx-background-radius: 20; -fx-border-radius: 20;"
                + "-fx-border-color: #dbeafe; -fx-effect: dropshadow(gaussian, rgba(37,99,235,0.10), 14, 0, 0, 5);");
        return box;
    }

    public static void showProfileDialog() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return;

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Hồ sơ tài khoản");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField username = field(user.getUsername(), true);
        TextField role = field(user.getRoleName(), true);
        TextField fullName = field(user.getFullName(), false);
        TextField phone = field(user.getPhoneNumber(), false);
        phone.setPromptText("Số điện thoại liên hệ");
        PasswordField newPassword = new PasswordField();
        newPassword.setPromptText("Để trống nếu không đổi");
        newPassword.setStyle("-fx-pref-height: 42; -fx-background-color: white; -fx-background-radius: 14;"
                + "-fx-border-radius: 14; -fx-border-color: " + BORDER + "; -fx-padding: 0 12;");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.addRow(0, new Label("Tên đăng nhập"), username);
        grid.addRow(1, new Label("Vai trò"), role);
        grid.addRow(2, new Label("Họ tên"), fullName);
        grid.addRow(3, new Label("Số điện thoại"), phone);
        grid.addRow(4, new Label("Mật khẩu mới"), newPassword);

        VBox root = new VBox(14,
                chip("PROFILE"),
                title("Hồ sơ tài khoản"),
                hint("Cập nhật họ tên, số điện thoại để người thắng/seller có thể liên hệ chính xác."),
                card(grid));
        root.setPadding(new Insets(18));
        dialog.getDialogPane().setContent(root);
        styleDialog(dialog.getDialogPane());

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            user.setFullName(fullName.getText());
            user.setPhoneNumber(phone.getText());
            user.setPassword(newPassword.getText());
            DataStorage.saveUsers(UserManager.getInstance().getAllUsers());
            SocketClient client = SessionManager.getInstance().getSocketClient();
            if (client != null && client.isConnected()) {
                client.updateProfile(fullName.getText(), phone.getText(), newPassword.getText());
            }
            showInfo("Hồ sơ", "Đã cập nhật hồ sơ tài khoản.");
        }
    }

    public static void showWalletDialog() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return;
        if (!user.supportsWallet()) {
            showInfo("Ví đấu giá", "Tài khoản này chưa hỗ trợ ví đấu giá.");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Ví đấu giá");
        ButtonType deposit = new ButtonType("Nạp tiền");
        ButtonType withdraw = new ButtonType("Rút tiền");
        dialog.getDialogPane().getButtonTypes().setAll(deposit, withdraw, ButtonType.CANCEL);

        Label balance = new Label(String.format("%,.0f VNĐ", user.getWalletBalance()));
        balance.setStyle("-fx-text-fill: " + BLUE + "; -fx-font-size: 34px; -fx-font-weight: bold;");
        VBox root = new VBox(14,
                chip("WALLET"),
                title("Ví đấu giá"),
                hint("Bidder dùng ví để đặt giá. Seller nhận tiền tự động khi phiên được thanh toán."),
                card(new Label("Số dư hiện tại"), balance));
        root.setPadding(new Insets(18));
        dialog.getDialogPane().setContent(root);
        styleDialog(dialog.getDialogPane());

        Optional<ButtonType> choice = dialog.showAndWait();
        if (choice.isEmpty() || choice.get() == ButtonType.CANCEL) return;

        TextField amountField = field("", false);
        amountField.setPromptText("Nhập số tiền VNĐ");
        Dialog<ButtonType> amountDialog = new Dialog<>();
        amountDialog.setTitle(choice.get() == deposit ? "Nạp tiền" : "Rút tiền");
        amountDialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        VBox form = new VBox(12, chip(choice.get() == deposit ? "DEPOSIT" : "WITHDRAW"),
                title(choice.get() == deposit ? "Nạp tiền vào ví" : "Rút tiền khỏi ví"),
                card(new Label("Số tiền"), amountField));
        form.setPadding(new Insets(18));
        amountDialog.getDialogPane().setContent(form);
        styleDialog(amountDialog.getDialogPane());

        Optional<ButtonType> ok = amountDialog.showAndWait();
        if (ok.isEmpty() || ok.get() != ButtonType.OK) return;

        try {
            double amount = Double.parseDouble(amountField.getText().replace(",", "").trim());
            SocketClient client = SessionManager.getInstance().getSocketClient();
            if (client != null && client.isConnected()) {
                if (choice.get() == deposit) client.deposit(amount);
                else client.withdraw(amount);
                showInfo("Ví đấu giá", "Đã gửi thao tác " + (choice.get() == deposit ? "nạp" : "rút")
                        + " tiền. Số dư sẽ cập nhật realtime nếu hợp lệ.");
            } else {
                showError("Ví đấu giá", "Không kết nối server nên không thể thao tác ví.");
            }
        } catch (Exception e) {
            showError("Ví đấu giá", e.getMessage());
        }
    }

    public static void showAuctionDetailDialog(String titleText, String payload) {
        String[] p = payload.split("\\|", -1);
        if (p.length < 10) return;
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(titleText);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);

        String price = formatMoney(p[2]);
        String start = formatMoney(p[4]);
        String seller = p[7].isBlank() ? "—" : p[7];
        String sellerPhone = p[8].isBlank() ? "—" : p[8];
        String leader = p[9].isBlank() ? "—" : p[9];

        GridPane info = new GridPane();
        info.setHgap(20); info.setVgap(10);
        info.addRow(0, new Label("Mã phiên"), bold(p[0]), new Label("Trạng thái"), chip(p[6]));
        info.addRow(1, new Label("Sản phẩm"), bold(p[1]), new Label("Danh mục"), bold(p[3]));
        info.addRow(2, new Label("Giá khởi điểm"), bold(start), new Label("Giá hiện tại"), bold(price));
        info.addRow(3, new Label("Seller"), bold(seller), new Label("SĐT Seller"), bold(sellerPhone));
        info.addRow(4, new Label("Leader/Winner"), bold(leader));

        VBox history = new VBox(8);
        if (p.length > 10 && !p[10].isBlank()) {
            for (String row : p[10].split(";")) {
                String[] b = row.split("#", -1);
                if (b.length >= 3) {
                    String phone = b.length > 3 && !b[3].isBlank() ? " • SĐT: " + b[3] : "";
                    Label line = new Label(b[0] + phone + " • " + formatMoney(b[1]) + " • " + b[2]);
                    line.setWrapText(true);
                    line.setStyle("-fx-text-fill: #334155; -fx-font-size: 13px; -fx-padding: 8 10; -fx-background-color: #f8fbff; -fx-background-radius: 12;");
                    history.getChildren().add(line);
                }
            }
        } else {
            history.getChildren().add(hint("Chưa có ai đặt giá."));
        }

        ScrollPane scroll = new ScrollPane(history);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(220);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        VBox root = new VBox(14,
                chip("AUCTION DETAIL"),
                title(titleText),
                card(info),
                title("Lịch sử đặt giá"),
                card(scroll));
        root.setPadding(new Insets(18));
        root.setPrefWidth(720);
        dialog.getDialogPane().setContent(root);
        styleDialog(dialog.getDialogPane());
        dialog.showAndWait();
    }

    public static void showBidderAuctionsDialog(String payload) {
        String[] sections = payload.split("::", -1);
        VBox joined = auctionRows(sections.length > 0 ? sections[0] : "EMPTY");
        VBox won = auctionRows(sections.length > 1 ? sections[1] : "EMPTY");
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Phiên của tôi");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        VBox root = new VBox(14,
                chip("MY AUCTIONS"),
                title("Phiên tôi đã tham gia"), card(joined),
                title("Phiên tôi đã thắng"), card(won));
        root.setPadding(new Insets(18));
        root.setPrefWidth(720);
        dialog.getDialogPane().setContent(root);
        styleDialog(dialog.getDialogPane());
        dialog.showAndWait();
    }

    private static VBox auctionRows(String data) {
        VBox box = new VBox(8);
        if (data == null || data.isBlank() || data.equals("EMPTY")) {
            box.getChildren().add(hint("Chưa có dữ liệu."));
            return box;
        }
        for (String row : data.split(",")) {
            String[] p = row.split("\\|", -1);
            if (p.length < 6) continue;
            Label line = new Label(p[0] + " — " + p[1] + "\nGiá: " + formatMoney(p[2]) + " • Trạng thái: " + p[5]
                    + (p.length > 6 && !p[6].isBlank() ? " • Seller: " + p[6] : "")
                    + (p.length > 7 && !p[7].isBlank() ? " • Leader/Winner: " + p[7] : ""));
            line.setWrapText(true);
            line.setStyle("-fx-text-fill: #334155; -fx-font-size: 13px; -fx-padding: 10 12; -fx-background-color: #f8fbff; -fx-background-radius: 14;");
            box.getChildren().add(line);
        }
        return box;
    }

    private static Label bold(String text) {
        Label l = new Label(text == null || text.isBlank() ? "—" : text);
        l.setWrapText(true);
        l.setStyle("-fx-text-fill: " + DARK + "; -fx-font-weight: bold;");
        return l;
    }

    private static String formatMoney(String value) {
        try { return String.format("%,.0f VNĐ", Double.parseDouble(value)); }
        catch (Exception e) { return value + " VNĐ"; }
    }

    public static void showInfo(String title, String content) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        Label body = hint(content);
        body.setStyle("-fx-text-fill: #334155; -fx-font-size: 14px; -fx-line-spacing: 4;");
        VBox root = new VBox(14, chip("INFO"), title(title), card(body));
        root.setPadding(new Insets(18));
        root.setPrefWidth(480);
        dialog.getDialogPane().setContent(root);
        styleDialog(dialog.getDialogPane());
        dialog.showAndWait();
    }

    public static void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        styleDialog(alert.getDialogPane());
        alert.showAndWait();
    }
}
