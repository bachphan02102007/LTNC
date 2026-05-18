package util.Factory;


import model.*;

public class ItemFactory {

    // static method → gọi trực tiếp ItemFactory.create(...) không cần new
    public static Item create(String type, String id, String name,
                              String description, double startingPrice,
                              String extraInfo) {
        // Validate dữ liệu đầu vào
        if (type == null || type.isBlank())
            throw new IllegalArgumentException("Loai san pham khong duoc de trong!");
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Ten san pham khong duoc de trong!");
        if (startingPrice <= 0)
            throw new IllegalArgumentException("Gia khoi diem phai lon hon 0!");

        switch (type.toUpperCase()) {
            case "ELECTRONICS":
            case "ELECTRIC":
                int warranty = 0;
                try {
                    warranty = Integer.parseInt(extraInfo == null || extraInfo.isBlank() ? "0" : extraInfo.trim());
                } catch (NumberFormatException ignored) {
                    warranty = 0; // Khi tạo nhanh từ UI Seller, không cần nhập bảo hành.
                }
                if (warranty < 0)
                    throw new IllegalArgumentException("Thoi gian bao hanh khong the am!");
                return new Electronics(id, name, description, startingPrice, warranty);
            case "ART":
                return new Art(id, name, description, startingPrice, extraInfo);
            case "VEHICLE":
                return new Vehicle(id, name, description, startingPrice, extraInfo);
            default:
                throw new IllegalArgumentException("Loai san pham khong hop le: " + type);
        }
    }
}