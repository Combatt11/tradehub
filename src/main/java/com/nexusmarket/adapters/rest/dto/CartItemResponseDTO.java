package com.nexusmarket.adapters.rest.dto;

import java.util.UUID;

/**
 * Linea de carrito expuesta por la API.
 */
public record CartItemResponseDTO(
        UUID variantId,
        int quantity
) {
}
