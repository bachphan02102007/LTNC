package util.Factory;


import model.*;

public class ItemFactory {

    // static method → gọi trực tiếp ItemFactory.create(...) không cần new
    public static Item create(String type, String id, String name,
                              String description, double startingPrice,
                              String extraInfo) {
        switch (type.toUpperCase()) {
            case "ELECTRONICS":
                return new Electronics(id, name, description,
                        startingPrice, Integer.parseInt(extraInfo));
            case "ART":
                return new Art(id, name, description, startingPrice, extraInfo);
            case "VEHICLE":
                return new Vehicle(id, name, description, startingPrice, extraInfo);
            default:
                throw new IllegalArgumentException("Loại sản phẩm không hợp lệ: " + type);
        }
    }
}