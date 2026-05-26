package test;

import model.Item;
import org.junit.jupiter.api.Test;
import util.Factory.ItemFactory;

import static org.junit.jupiter.api.Assertions.*;

class ItemFactoryTest {

    // Test 1: Kiểm tra việc khởi tạo đối tượng thuộc loại Điện tử (Electronics) hợp lệ qua Factory thành công.
    @Test
    void testCreate_Electronics_ShouldSucceed() {
        // Bước 1: Gọi hàm Factory để tạo một đối tượng Item kiểu "ELECTRONICS" với đầy đủ tham số hợp lệ
        Item item = ItemFactory.create("ELECTRONICS","I001","Laptop","Mo ta",500.0,"12");
        // Bước 2: Xác nhận đối tượng được tạo ra thành công (không bị null)
        assertNotNull(item);
        // Bước 3: Kiểm tra xem thuộc tính tên của Item có trùng khớp với giá trị khởi tạo hay không
        assertEquals("Laptop", item.getName());
    }

    // Test 2: Đảm bảo hệ thống ném ra ngoại lệ khi cố tình truyền vào một loại mặt hàng (Type) không hợp lệ.
    @Test
    void testCreate_InvalidType_ShouldThrow() {
        // Bước 1, 2 & 3: Truyền vào loại mặt hàng không xác định ("UNKNOWN") và xác nhận hệ thống chặn lại bằng lỗi IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () ->
                ItemFactory.create("UNKNOWN","I002","X","Y",100.0,"Z"));
    }

    // Test 3: Chặn đứng hành vi khởi tạo mặt hàng với mức giá khởi điểm mang giá trị âm.
    @Test
    void testCreate_NegativePrice_ShouldThrow() {
        // Bước 1, 2 & 3: Cố tình truyền mức giá âm (-100.0) và kiểm tra xem hệ thống có ném ra lỗi IllegalArgumentException không
        assertThrows(IllegalArgumentException.class, () ->
                ItemFactory.create("ART","I003","Tranh","Mo ta",-100.0,"HoaSi"));
    }

    // Test 4: Ngăn chặn việc tạo sản phẩm đấu giá có chuỗi tên bị để trống.
    @Test
    void testCreate_BlankName_ShouldThrow() {
        // Bước 1, 2 & 3: Cố tình truyền chuỗi tên rỗng ("") và đảm bảo hệ thống ném ra ngoại lệ IllegalArgumentException phù hợp
        assertThrows(IllegalArgumentException.class, () ->
                ItemFactory.create("ART","I004","","Mo ta",100.0,"HoaSi"));
    }
}