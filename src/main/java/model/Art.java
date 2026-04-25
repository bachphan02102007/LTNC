package model;

public class Art extends Item {
    private String artistName; // Thuộc tính riêng: tên họa sĩ/nghệ nhân

    public Art(String id, String name, String description, double startingPrice, String artistName) {
        super(id, name, description, startingPrice);
        this.artistName = artistName;
    }

    @Override
    public String printInfo() {
        return "Art [" + this.id + "] " + this.name + " by " + this.artistName + " - Giá khởi điểm: $" + this.startingPrice;
    }
}
