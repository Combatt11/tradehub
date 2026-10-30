package com.nexusmarket.adapters.rest.dto;

import java.util.List;
import java.util.UUID;

/**
 * Representacion publica de un carrito.
 */
public record CartResponseDTO(
        UUID id,
        UUID buyerId,
        List<CartItemResponseDTO> items
) {
}
