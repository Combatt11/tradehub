package com.nexusmarket.adapters.persistence.jpa;

import com.nexusmarket.domain.models.InventoryItem;

import java.util.Objects;

/**
 * Traduccion entre {@link InventoryItem} y {@link InventoryItemEntity}.
 */
public final class InventoryMapper {

    private InventoryMapper() {
    }

    public static InventoryItem toDomain(InventoryItemEntity entity) {
        Objects.requireNonNull(entity, "entity must not be null");

        return new InventoryItem(
                entity.getVariantId(),
                entity.getWarehouseId(),
                entity.getAvailableStock(),
                entity.getReservedStock()
        );
    }

    public static InventoryItemEntity toEntity(InventoryItem item) {
        Objects.requireNonNull(item, "item must not be null");

        InventoryItemEntity entity = new InventoryItemEntity();
        copyScalars(entity, item);
        return entity;
    }

    /**
     * Actualiza una entidad ya gestionada sin modificar su clave primaria.
     */
    public static void updateEntity(InventoryItemEntity entity, InventoryItem item) {
        Objects.requireNonNull(entity, "entity must not be null");
        Objects.requireNonNull(item, "item must not be null");
        copyScalars(entity, item);
    }

    private static void copyScalars(InventoryItemEntity entity, InventoryItem item) {
        entity.setVariantId(item.getVariantId());
        entity.setWarehouseId(item.getWarehouseId());
        entity.setAvailableStock(item.getAvailableStock());
        entity.setReservedStock(item.getReservedStock());
    }
}
