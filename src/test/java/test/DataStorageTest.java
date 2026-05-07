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

    @Test
    void testSaveAndLoadList_ShouldReturnSameData() {
        Item laptop = ItemFactory.create("ELECTRONICS","I001",
                "Laptop","Mo ta",500.0,"12");
        Auction auction = new Auction("A001", laptop,
                LocalDateTime.now().plusHours(1));

        DataStorage.saveList(List.of(auction), TEST_FILE);

        List<Auction> loaded = DataStorage.loadList(TEST_FILE);
        assertEquals(1, loaded.size());
        assertEquals("A001", loaded.get(0).getAuctionId());
    }

    @Test
    void testLoadList_FileNotExist_ShouldReturnEmptyList() {
        List<Object> result = DataStorage.loadList("data/khong_ton_tai.dat");
        assertNotNull(result);
        assertEquals(0, result.size());
    }
}