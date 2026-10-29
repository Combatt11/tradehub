package com.nexusmarket.domain.ports.in;

import com.nexusmarket.domain.models.InventoryItem;

import java.util.UUID;

/**
 * Puerto de entrada para reservar stock de una variante en un almacen.
 */
public interface ReserveInventoryUseCase {

    /**
     * Reserva la cantidad indicada y persiste el inventario actualizado.
     *
     * @param variantId   variante/SKU
     * @param warehouseId almacen
     * @param quantity    unidades a reservar
     * @return inventario actualizado
     */
    InventoryItem reserveStock(UUID variantId, UUID warehouseId, int quantity);
}
