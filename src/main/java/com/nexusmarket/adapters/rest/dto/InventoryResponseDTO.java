package com.nexusmarket.adapters.rest.dto;

import java.util.UUID;

/**
 * Representacion publica del inventario actualizado.
 */
public record InventoryResponseDTO(
        UUID variantId,
        UUID warehouseId,
        int availableStock,
        int reservedStock
) {
}
