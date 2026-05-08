package model;
import java.io.Serializable;

public abstract class Item implements Serializable {
    protected String id;
    protected String name;
    protected String description;
    protected double startingPrice;
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
