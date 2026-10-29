package com.nexusmarket.domain.models;

import java.util.UUID;

/**
 * Se lanza cuando una reserva de inventario excede el stock disponible.
 */
public final class InsufficientStockException extends RuntimeException {

    private final UUID variantId;
    private final UUID warehouseId;
    private final int availableStock;
    private final int requestedQuantity;

    public InsufficientStockException(
            UUID variantId,
            UUID warehouseId,
            int availableStock,
            int requestedQuantity
    ) {
        super("Insufficient stock for variant " + variantId
                + " in warehouse " + warehouseId
                + ": requested " + requestedQuantity
                + ", available " + availableStock);
        this.variantId = variantId;
        this.warehouseId = warehouseId;
        this.availableStock = availableStock;
        this.requestedQuantity = requestedQuantity;
    }

    public UUID getVariantId() {
        return variantId;
    }

    public UUID getWarehouseId() {
        return warehouseId;
    }

    public int getAvailableStock() {
        return availableStock;
    }

    public int getRequestedQuantity() {
        return requestedQuantity;
    }
}
