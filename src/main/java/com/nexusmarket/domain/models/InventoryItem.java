package com.nexusmarket.domain.models;

import java.util.Objects;
import java.util.UUID;

/**
 * Inventario por variante y almacen.
 *
 * <p>{@code availableStock} representa la cantidad disponible y
 * {@code reservedStock} la cantidad apartada. El stock fisico total es la suma
 * de ambos. Al reservar, la cantidad se mueve de disponible a reservado; al
 * confirmar el pago, el reservado se descuenta definitivamente.</p>
 */
public final class InventoryItem {

    private final UUID variantId;
    private final UUID warehouseId;
    private int availableStock;
    private int reservedStock;

    public InventoryItem(UUID variantId, UUID warehouseId, int availableStock, int reservedStock) {
        this.variantId = Objects.requireNonNull(variantId, "variantId must not be null");
        this.warehouseId = Objects.requireNonNull(warehouseId, "warehouseId must not be null");

        if (availableStock < 0) {
            throw new IllegalArgumentException("availableStock must not be negative");
        }
        if (reservedStock < 0) {
            throw new IllegalArgumentException("reservedStock must not be negative");
        }

        this.availableStock = availableStock;
        this.reservedStock = reservedStock;
    }

    /**
     * Reserva stock para una linea de pedido.
     *
     * @throws InsufficientStockException si no hay disponible suficiente
     */
    public void reserve(int quantity) {
        requirePositive(quantity);

        if (availableStock < quantity) {
            throw new InsufficientStockException(variantId, warehouseId, availableStock, quantity);
        }

        int newReservedStock = Math.addExact(reservedStock, quantity);
        availableStock -= quantity;
        reservedStock = newReservedStock;
    }

    /**
     * Confirma una reserva al capturar el pago. El stock reservado se descuenta
     * definitivamente (RN-INV-07).
     */
    public void confirmReservation(int quantity) {
        requirePositive(quantity);
        if (reservedStock < quantity) {
            throw new IllegalStateException(
                    "Cannot confirm " + quantity + " units; reserved stock is " + reservedStock);
        }
        reservedStock -= quantity;
    }

    /**
     * Libera una reserva activa y devuelve las unidades a disponible
     * (RN-INV-08).
     */
    public void releaseReservation(int quantity) {
        requirePositive(quantity);
        if (reservedStock < quantity) {
            throw new IllegalStateException(
                    "Cannot release " + quantity + " units; reserved stock is " + reservedStock);
        }

        int newAvailableStock = Math.addExact(availableStock, quantity);
        reservedStock -= quantity;
        availableStock = newAvailableStock;
    }

    /**
     * Repone stock fisico, por ejemplo tras una cancelacion pagada o una
     * devolucion aceptada.
     */
    public void restock(int quantity) {
        requirePositive(quantity);
        availableStock = Math.addExact(availableStock, quantity);
    }

    public int onHandStock() {
        return Math.addExact(availableStock, reservedStock);
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

    public int getReservedStock() {
        return reservedStock;
    }

    private static void requirePositive(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be greater than zero");
        }
    }
}
