package com.nexusmarket.domain.ports.in;

import java.util.Objects;
import java.util.UUID;

/**
 * Item de carrito que participa en el checkout multi-vendedor.
 */
public record CheckoutItemDTO(UUID variantId, int quantity) {

    public CheckoutItemDTO {
        Objects.requireNonNull(variantId, "variantId must not be null");
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be greater than zero");
        }
    }
}
