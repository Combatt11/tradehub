package com.nexusmarket.adapters.rest.dto;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Representacion publica de una variante/SKU.
 */
public record VariantResponseDTO(
        UUID id,
        String sku,
        BigDecimal price,
        String currency,
        Map<String, String> attributes
) {
}
