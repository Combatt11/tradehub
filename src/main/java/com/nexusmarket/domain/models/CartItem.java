package com.nexusmarket.domain.models;

import java.util.Objects;
import java.util.UUID;

/**
 * Linea de un carrito de compras.
 */
public record CartItem(UUID variantId, int quantity) {

    public static final int MAX_QUANTITY_PER_LINE = 99;

    public CartItem {
        Objects.requireNonNull(variantId, "variantId must not be null");
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be greater than zero");
        }
        if (quantity > MAX_QUANTITY_PER_LINE) {
            throw new IllegalArgumentException(
                    "quantity must not exceed " + MAX_QUANTITY_PER_LINE);
        }
    }
}
