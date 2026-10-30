package com.nexusmarket.domain.models;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class InventoryItemTest {

    private final UUID variantId = UUID.randomUUID();
    private final UUID warehouseId = UUID.randomUUID();

    @Test
    void reserveThrowsWhenThereIsNotEnoughStock() {
        InventoryItem item = new InventoryItem(variantId, warehouseId, 5, 0);

        assertThrows(InsufficientStockException.class, () -> item.reserve(6));

        assertEquals(5, item.getAvailableStock());
        assertEquals(0, item.getReservedStock());
    }

    @Test
    void addStockAddsToAvailableStock() {
        InventoryItem item = new InventoryItem(variantId, warehouseId, 10, 2);

        item.addStock(5);

        assertEquals(15, item.getAvailableStock());
        assertEquals(2, item.getReservedStock());
        assertEquals(17, item.onHandStock());
    }

    @Test
    void releaseReservationReturnsStockToAvailable() {
        InventoryItem item = new InventoryItem(variantId, warehouseId, 10, 5);

        item.releaseReservation(3);

        assertEquals(13, item.getAvailableStock());
        assertEquals(2, item.getReservedStock());
    }

    @Test
    void restockAddsStockOnlyOnce() {
        InventoryItem item = new InventoryItem(variantId, warehouseId, 10, 0);

        item.restock(5);

        assertEquals(15, item.getAvailableStock());
    }
}
