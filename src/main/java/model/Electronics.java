package model;

public class Electronics  extends Item {
    private int bao_hanh;
    public Electronics(String id, String name, String description, double startingPrice, int bao_hanh) {
        super(id, name, description, startingPrice);
        this.bao_hanh = bao_hanh;
    }

    @Override
    public String printInfo() {
        return "Electronics[" + this.id + "] " + this.name + " - Giá khởi điểm: $" +
                this.startingPrice + " - Bảo hành: " + this.bao_hanh + " tháng.";
    }
}
