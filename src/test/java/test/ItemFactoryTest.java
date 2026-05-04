package test;

import model.Item;
import org.junit.jupiter.api.Test;
import util.Factory.ItemFactory;

import static org.junit.jupiter.api.Assertions.*;

class ItemFactoryTest {

    @Test
    void testCreate_Electronics_ShouldSucceed() {
        Item item = ItemFactory.create("ELECTRONICS","I001","Laptop","Mo ta",500.0,"12");
        assertNotNull(item);
        assertEquals("Laptop", item.getName());
    }

    @Test
    void testCreate_InvalidType_ShouldThrow() {
        assertThrows(IllegalArgumentException.class, () ->
                ItemFactory.create("UNKNOWN","I002","X","Y",100.0,"Z"));
    }

    @Test
    void testCreate_NegativePrice_ShouldThrow() {
        assertThrows(IllegalArgumentException.class, () ->
                ItemFactory.create("ART","I003","Tranh","Mo ta",-100.0,"HoaSi"));
    }

    @Test
    void testCreate_BlankName_ShouldThrow() {
        assertThrows(IllegalArgumentException.class, () ->
                ItemFactory.create("ART","I004","","Mo ta",100.0,"HoaSi"));
    }
}