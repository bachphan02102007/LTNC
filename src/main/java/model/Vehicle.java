package model;

public class Vehicle extends Item {
    private String engineType;
    public Vehicle(String id, String name, String description, double startingPrice, String engineType) {
        super(id, name, description, startingPrice);
        this.engineType = engineType;
    }

    @Override
    public String printInfo() {
        return "Vehicle[" + this.id + "] " + this.name + " - Động cơ: " + this.engineType + " - Giá khởi điểm: $" + this.startingPrice;
    }
}
