package model;

public enum AuctionStatus { // trạng thái đấu giá
    OPEN,       // Đã tạo, chờ bắt đầu
    RUNNING,    // Đang diễn ra
    FINISHED,   // Hết giờ, chờ thanh toán
    PAID,       // Đã thanh toán xong
    CANCELED    // Bị hủy
}
