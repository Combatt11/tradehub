package com.nexusmarket.adapters.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio Spring Data JPA para inventario.
 */
public interface JpaInventoryRepository extends JpaRepository<InventoryItemEntity, Long> {

    Optional<InventoryItemEntity> findByVariantIdAndWarehouseId(UUID variantId, UUID warehouseId);
}
