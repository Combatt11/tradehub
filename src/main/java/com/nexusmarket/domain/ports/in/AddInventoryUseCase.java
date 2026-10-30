package com.nexusmarket.domain.ports.in;

import com.nexusmarket.domain.models.InventoryItem;

import java.util.UUID;

/**
 * Puerto de entrada para aprovisionar inventario.
 */
public interface AddInventoryUseCase {

    /**
     * Añade stock disponible para una variante y almacen. Si no existe el
     * registro de inventario, se crea.
     *
     * @param variantId   variante a aprovisionar
     * @param warehouseId almacen destino
     * @param quantity    unidades a añadir
     * @return inventario actualizado
     */
    InventoryItem addStock(UUID variantId, UUID warehouseId, int quantity);
}
