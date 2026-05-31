# BÁO CÁO BÀI TẬP LỚN LẬP TRÌNH NÂNG CAO

**Tên đề tài:** Online Auction System  
**Môn học:** Lập trình nâng cao (LTNC)  
**Thành viên:** Phan Trọng Bách, Nguyễn Tá Mạnh, Vũ Minh Diện, Lê Thành Duy.
                

## 1. Mục tiêu và phạm vi thực hiện

Đề tài xây dựng hệ thống đấu giá trực tuyến theo mô hình client-server. Hệ thống cho phép Seller đăng phiên đấu giá, Bidder đặt giá theo thời gian thực, Admin quản lý người dùng/phiên đấu giá, đồng thời xử lý tình huống nhiều client đặt giá cùng lúc.

Phạm vi thực hiện gồm: ứng dụng desktop JavaFX, server TCP socket phục vụ nhiều client, phân quyền Admin/Seller/Bidder, quản lý ví đấu giá, lưu dữ liệu cục bộ bằng Java Serialization, realtime update qua Socket/Observer và kiểm thử logic quan trọng bằng JUnit.

## 2. Kiến trúc tổng thể

**JavaFX Client (FXML + Controller)** → **SocketClient** → **TCP Socket** → **AuctionServer** → **ClientHandler** → **Manager / Model / DataStorage**

Luồng xử lý chính: người dùng thao tác trên giao diện JavaFX; Controller gọi SocketClient gửi command đến server; AuctionServer tạo ClientHandler xử lý từng client; nghiệp vụ được thực hiện trên các lớp model/manager; kết quả được lưu qua DataStorage và broadcast realtime về các client liên quan.

## 3. Bảng đối chiếu barem chấm điểm

| Nhóm tiêu chí | Nội dung theo barem | Điểm | Minh chứng trong project | Tự đánh giá |
|---|---:|---:|---|---|
| Thiết kế lớp & cây kế thừa | Xác định và triển khai các lớp chính: User, Bidder/Seller/Admin, Item, Auction, BidTransaction,... | 0.5 | `src/main/java/model`: User, Bidder, Seller, Admin, Item, Auction, BidTransaction; phân vai rõ theo nghiệp vụ. | Đạt |
| Thiết kế lớp & cây kế thừa | Áp dụng đúng OOP: Encapsulation, Inheritance, Polymorphism, Abstraction. | 1.0 | Thuộc tính private, getter/setter; User là lớp cha; Bidder/Seller/Admin kế thừa; Item trừu tượng; đa hình qua User/Item/WalletOwner. | Đạt |
| Thiết kế lớp & cây kế thừa | Áp dụng design pattern phù hợp. | 1.0 | Singleton: UserManager, AuctionManager, SessionManager; Factory Method: ItemFactory; Observer: AuctionObserver/ServerBroadcastObserver; MVC ở JavaFX. | Đạt |
| Chức năng chính | Quản lý người dùng, sản phẩm. | 1.0 | Đăng ký, đăng nhập, phân quyền, cập nhật hồ sơ, khóa/mở khóa user; Seller đăng sản phẩm/phiên; Admin quản lý phiên. | Đạt |
| Chức năng chính | Chức năng đấu giá. | 1.0 | Danh sách phiên, chi tiết phiên, đặt giá, xác định người thắng, trạng thái OPEN/RUNNING/FINISHED/CANCELED/PAID, lưu lịch sử bid. | Đạt |
| Chức năng chính | Xử lí lỗi và ngoại lệ. | 1.0 | InvalidBidException, AuctionClosedException, AuthenticationException; kiểm tra role, số dư ví, tài khoản bị khóa, input sai, phiên kết thúc. | Đạt |
| Kỹ thuật quan trọng & concurrency | Xử lí đấu giá đồng thời an toàn, tránh lost update/race condition. | 1.0 | Auction dùng ReentrantLock để bảo vệ placeBid/getHighestBid/getHistory; AuctionConcurrencyTest dùng ExecutorService/CountDownLatch để kiểm thử. | Đạt |
| Kỹ thuật quan trọng & concurrency | Realtime update bằng Observer/Socket: thông báo bid mới cho tất cả client. | 0.5 | AuctionObserver nhận sự kiện; ServerBroadcastObserver gọi AuctionServer.broadcastAll; SocketClient lắng nghe và cập nhật UI bằng Platform.runLater. | Đạt |
| Tích hợp, kiến trúc & chất lượng mã | Thiết kế kiến trúc Client-Server rõ ràng. | 0.5 | AuctionServer mở port 9999; ClientHandler xử lý từng client; client gửi command dạng text qua TCP socket. | Đạt |
| Tích hợp, kiến trúc & chất lượng mã | Áp dụng MVC cho client và tách Model/Controller/DataStorage. | 0.5 | FXML nằm trong resources/view; controller nằm trong controller; model nằm trong model; DataStorage phụ trách đọc/ghi dữ liệu. | Đạt |
| Tích hợp, kiến trúc & chất lượng mã | Sử dụng Maven/Gradle, coding convention tốt, mã nguồn sạch. | 0.5 | `pom.xml` cấu hình JavaFX, JUnit, shade plugin; source chia package controller/model/network/util/exception/test. | Đạt |
| Tích hợp, kiến trúc & chất lượng mã | Unit Test cho logic quan trọng. | 0.5 | `src/test/java/test` có AuctionTest, AuctionConcurrencyTest, UserManagerTest, DataStorageTest, exception tests, ItemFactoryTest,... | Đạt |
| Tích hợp, kiến trúc & chất lượng mã | CI/CD cơ bản: GitHub Actions + test tự động. | 0.5 | `.github/workflows/ci.yml` chạy Maven test/package khi push/pull request. | Đạt |


## 4. Tóm tắt chức năng.

| Chức năng | Hướng giải quyết | Lý do lựa chọn |
|---|---|---|
| Đăng nhập/đăng ký/phân quyền | Dùng UserManager quản lý người dùng; User là lớp cha, Bidder/Seller/Admin là lớp con. | Dễ mở rộng vai trò và phù hợp yêu cầu thiết kế cây kế thừa. |
| Đấu giá realtime | Client gửi BID qua Socket; server kiểm tra nghiệp vụ, cập nhật Auction và broadcast kết quả. | Thể hiện rõ lập trình mạng, nhiều client và cập nhật thời gian thực. |
| Concurrent bidding | Bọc vùng đặt giá trong ReentrantLock và kiểm thử bằng nhiều thread. | Tránh lost update/race condition khi nhiều bidder đặt giá cùng lúc. |
| Lưu dữ liệu | DataStorage serialize users.dat và auctions.dat. | Đủ đơn giản cho bài tập lớn, vẫn giữ dữ liệu sau khi tắt server. |
| Kiểm thử/CI | JUnit test cho logic quan trọng; GitHub Actions chạy test tự động. | Tăng độ tin cậy và đáp ứng yêu cầu chất lượng mã. |

