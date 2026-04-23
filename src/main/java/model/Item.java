package model;

public abstract class Item {
    protected String id;
    protected String name;
    protected String description; // mô tả
    protected double startingPrice; //giá khởi điểm;
    public Item(String id, String name, String description, double startingPrice) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public double getStartingPrice() {
        return startingPrice;
    }

    public abstract String printInfo();
}
