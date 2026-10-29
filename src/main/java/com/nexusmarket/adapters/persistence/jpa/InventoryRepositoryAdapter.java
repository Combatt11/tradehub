package com.nexusmarket.adapters.persistence.jpa;

import com.nexusmarket.domain.models.InventoryItem;
import com.nexusmarket.domain.ports.out.InventoryRepositoryPort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador de salida que implementa {@link InventoryRepositoryPort} mediante JPA.
 */
@Repository
public class InventoryRepositoryAdapter implements InventoryRepositoryPort {

    private final JpaInventoryRepository inventoryRepository;

    public InventoryRepositoryAdapter(JpaInventoryRepository inventoryRepository) {
        this.inventoryRepository = Objects.requireNonNull(
                inventoryRepository, "inventoryRepository must not be null");
    }

    @Override
    @Transactional
    public InventoryItem save(InventoryItem inventoryItem) {
        Objects.requireNonNull(inventoryItem, "inventoryItem must not be null");

        InventoryItemEntity entity = inventoryRepository
                .findByVariantIdAndWarehouseId(
                        inventoryItem.getVariantId(),
                        inventoryItem.getWarehouseId())
                .map(existing -> {
                    InventoryMapper.updateEntity(existing, inventoryItem);
                    return existing;
                })
                .orElseGet(() -> InventoryMapper.toEntity(inventoryItem));

        return InventoryMapper.toDomain(inventoryRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<InventoryItem> findByVariantAndWarehouse(UUID variantId, UUID warehouseId) {
        Objects.requireNonNull(variantId, "variantId must not be null");
        Objects.requireNonNull(warehouseId, "warehouseId must not be null");

        return inventoryRepository.findByVariantIdAndWarehouseId(variantId, warehouseId)
                .map(InventoryMapper::toDomain);
    }
}
