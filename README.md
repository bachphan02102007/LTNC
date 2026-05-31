# Online Auction System - LTNC

## 1. Mô tả bài toán và phạm vi hệ thống

Online Auction System là hệ thống đấu giá trực tuyến theo mô hình client-server. Hệ thống cho phép người bán đăng sản phẩm/phiên đấu giá, người mua tham gia đặt giá theo thời gian thực, quản trị viên quản lý tài khoản và phiên đấu giá. Dữ liệu người dùng và phiên đấu giá được lưu cục bộ bằng Java Serialization.

Phạm vi thực hiện:

- Ứng dụng desktop JavaFX cho client.
- Server socket TCP xử lý nhiều client đồng thời.
- Phân quyền người dùng: Admin, Seller, Bidder.
- Đấu giá realtime, lưu lịch sử đặt giá, ví người dùng và quản lý phiên.
- Kiểm thử đơn vị và kiểm thử tình huống concurrent bidding.

## 2. Công nghệ sử dụng, môi trường chạy và yêu cầu cài đặt


- Java 17
- JavaFX 21.0.2
- Maven
- TCP Socket
- Java Serialization
- JUnit 5
- GitHub Actions CI

Yêu cầu cài đặt:

- JDK 17 trở lên
- Maven 3.8+ hoặc dùng Maven Wrapper đi kèm project
- Hệ điều hành có hỗ trợ JavaFX tương ứng với môi trường build

Kiểm tra Java:

```bash
java -version
```

## 3. Cấu trúc thư mục/module chính

```text
src/main/java
├── controller/        # Controller JavaFX cho các màn hình
├── exception/         # Exception nghiệp vụ đấu giá/đăng nhập
├── main/              # Launcher cho client fat JAR
├── model/             # Entity và logic nghiệp vụ: User, Auction, Item...
├── network/           # AuctionServer, ClientHandler, SocketClient
├── util/              # DataStorage, SessionManager, Singleton Manager, Factory
└── view/              # HelloApp - entry point JavaFX

src/main/resources/view
├── login.fxml
├── register.fxml
├── auction_list.fxml
├── auction_room.fxml
├── seller_dashboard.fxml
└── admin_dashboard.fxml

src/test/java/test       # JUnit tests
data/                    # users.dat, auctions.dat
.github/workflows/       # CI workflow
```

## 4. Build project và vị trí file JAR

Build bằng Maven:

```bash
mvn clean package
```

Hoặc dùng Maven Wrapper:

```bash
./mvnw clean package
```

Sau khi build thành công, các file JAR nằm trong thư mục `target/`:

```text
target/OnlineAuctionSystem-1.0-SNAPSHOT-server.jar
target/OnlineAuctionSystem-1.0-SNAPSHOT-client.jar
```

## 5. Hướng dẫn chạy Server/Client

### Bước 1: Chạy server

Mở terminal thứ nhất tại thư mục gốc project:

```bash
java -jar target/OnlineAuctionSystem-1.0-SNAPSHOT-server.jar
```

Server mặc định chạy tại:

```text
host: localhost
port: 9999
```

### Bước 2: Chạy client

Mở terminal thứ hai:

```bash
java -jar target/OnlineAuctionSystem-1.0-SNAPSHOT-client.jar
```

Để chạy nhiều client, mở thêm terminal khác và chạy lại lệnh client:

```bash
java -jar target/OnlineAuctionSystem-1.0-SNAPSHOT-client.jar
```

### Tài khoản mặc định

Khi server khởi động, nếu chưa có admin, hệ thống tự tạo:

```text
username: admin
password: admin
role: ADMIN
```

Các tài khoản Seller/Bidder có thể được tạo qua màn hình đăng ký của ứng dụng.

## 6. Danh sách chức năng đã hoàn thành

### Chức năng chung

- Đăng nhập, đăng ký tài khoản.
- Phân quyền Admin, Seller, Bidder.
- Cập nhật thông tin hồ sơ tài khoản.
- Lưu dữ liệu người dùng và phiên đấu giá bằng file `.dat`.

### Chức năng Seller

- Đăng sản phẩm/phiên đấu giá.
- Xem danh sách phiên do mình đăng.
- Xóa phiên đấu giá của mình.
- Theo dõi trạng thái phiên và lịch sử đặt giá.

### Chức năng Bidder

- Xem danh sách phiên đấu giá đang hiển thị.
- Đặt giá theo thời gian thực.
- Xem chi tiết phiên đấu giá.
- Quản lý ví: xem số dư, nạp tiền, rút tiền.
- Xem danh sách phiên đã tham gia và phiên đã thắng.

### Chức năng Admin

- Đăng nhập bằng tài khoản admin mặc định.
- Khóa/mở khóa tài khoản người dùng.
- Hủy phiên đấu giá.
- Xóa phiên đấu giá.

### Chức năng kỹ thuật

- Server socket TCP xử lý nhiều client cùng lúc bằng `ExecutorService`.
- Realtime update qua cơ chế broadcast từ server đến client.
- Đồng bộ danh sách phiên khi có phiên mới, đặt giá mới, xóa/hủy phiên.
- Xử lý concurrent bidding bằng `ReentrantLock` trong model `Auction`.
- Áp dụng một số design patterns: Singleton, Factory Method, Observer, MVC.
- Có JUnit tests cho model, storage, user manager, session và concurrent bidding.

## 7. Báo cáo PDF và video demo

> Cập nhật link trước khi nộp chính thức.

- Link báo cáo PDF: `https://drive.google.com/file/d/1QwJnz_mZXvMk438ZgiH6txxlS6TWRIsO/view?usp=sharing`
- Link video demo: `TODO: dán link video demo tại đây`

