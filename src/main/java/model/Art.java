package model;

public class Art extends Item {
    private String ten_hoa_si;
    private int age;
    public Art(String id, String name, String description, double startingPrice, String ten_hoa_si, int age) {
        super(id, name, description, startingPrice);
        this.ten_hoa_si = ten_hoa_si;
        this.age = age;
    }

    @Override
    public String printInfo() {
        return "Art[" + this.id + "] " + this.name + " - Artis: " + this.ten_hoa_si + " - Age: " + this.age + " - Giá khởi điểm: $" +
                this.startingPrice ;
    }
}
