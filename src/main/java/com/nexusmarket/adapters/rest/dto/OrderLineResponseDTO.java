package com.nexusmarket.adapters.rest.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Linea de pedido devuelta por la API.
 */
public record OrderLineResponseDTO(
        UUID variantId,
        int quantity,
        BigDecimal unitPrice,
        String currency,
        BigDecimal lineTotal
) {
}
