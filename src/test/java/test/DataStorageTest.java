package test;

import model.*;
import org.junit.jupiter.api.*;
import util.DataStorage;
import util.Factory.ItemFactory;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DataStorageTest {

    private static final String TEST_FILE = "data/test_auctions.dat";

    @AfterEach
    void cleanup() {
        // Xóa file test sau mỗi test
        new File(TEST_FILE).delete();
    }

    // Test 1: Kiểm tra việc lưu danh sách đấu giá xuống file và đọc lên lại, đảm bảo tính toàn vẹn của dữ liệu.
    @Test
    void testSaveAndLoadList_ShouldReturnSameData() {
        // Bước 1: Khởi tạo các đối tượng Item và Auction mẫu để làm dữ liệu thử nghiệm
        Item laptop = ItemFactory.create("ELECTRONICS","I001",
                "Laptop","Mo ta",500.0,"12");
        Auction auction = new Auction("A001", laptop,
                LocalDateTime.now().plusHours(1));

        // Bước 2: Thực hiện lưu danh sách chứa phiên đấu giá xuống file dữ liệu tạm thời
        DataStorage.saveList(List.of(auction), TEST_FILE);

        // Bước 3: Tải lại danh sách từ file và xác nhận số lượng phần tử cũng như thông tin ID trùng khớp hoàn toàn
        List<Auction> loaded = DataStorage.loadList(TEST_FILE);
        assertEquals(1, loaded.size());
        assertEquals("A001", loaded.get(0).getAuctionId());
    }

    // Test 2: Đảm bảo hệ thống trả về một danh sách rỗng (khác null) khi đọc từ một đường dẫn file không tồn tại.
    @Test
    void testLoadList_FileNotExist_ShouldReturnEmptyList() {
        // Bước 1 & 2: Thực hiện gọi hàm tải danh sách từ một đường dẫn file hoàn toàn không tồn tại trong hệ thống
        List<Object> result = DataStorage.loadList("data/khong_ton_tai.dat");

        // Bước 3: Xác nhận kết quả nhận được không bị null và phải là một danh sách trống rỗng (size = 0)
        assertNotNull(result);
        assertEquals(0, result.size());
    }
}