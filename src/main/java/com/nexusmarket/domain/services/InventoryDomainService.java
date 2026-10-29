package com.nexusmarket.domain.services;

import com.nexusmarket.domain.models.InventoryItem;
import com.nexusmarket.domain.ports.in.ReserveInventoryUseCase;
import com.nexusmarket.domain.ports.out.InventoryRepositoryPort;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;

/**
 * Servicio de dominio para reservas de inventario.
 */
public final class InventoryDomainService implements ReserveInventoryUseCase {

    private final InventoryRepositoryPort inventoryRepository;

    public InventoryDomainService(InventoryRepositoryPort inventoryRepository) {
        this.inventoryRepository = Objects.requireNonNull(
                inventoryRepository,
                "inventoryRepository must not be null");
    }

    @Override
    public InventoryItem reserveStock(UUID variantId, UUID warehouseId, int quantity) {
        Objects.requireNonNull(variantId, "variantId must not be null");
        Objects.requireNonNull(warehouseId, "warehouseId must not be null");

        InventoryItem inventoryItem = inventoryRepository
                .findByVariantAndWarehouse(variantId, warehouseId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Inventory not found for variant " + variantId
                                + " and warehouse " + warehouseId));

        inventoryItem.reserve(quantity);
        return inventoryRepository.save(inventoryItem);
    }
}
