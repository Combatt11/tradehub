package com.nexusmarket.domain.ports.out;

import com.nexusmarket.domain.models.InventoryItem;

import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida para la persistencia de inventario.
 */
public interface InventoryRepositoryPort {

    InventoryItem save(InventoryItem inventoryItem);

    Optional<InventoryItem> findByVariantAndWarehouse(UUID variantId, UUID warehouseId);
}
