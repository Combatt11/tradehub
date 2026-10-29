package com.nexusmarket.domain.models;

import java.util.Objects;
import java.util.UUID;

/**
 * Linea de pedido con precio congelado.
 */
public record OrderLine(
        UUID variantId,
        int quantity,
        Money unitPrice
) {

    public OrderLine {
        Objects.requireNonNull(variantId, "variantId must not be null");
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be greater than zero");
        }
        Objects.requireNonNull(unitPrice, "unitPrice must not be null").requirePositive("unitPrice");
    }

    public Money lineTotal() {
        return unitPrice.multiply(quantity);
    }
}
