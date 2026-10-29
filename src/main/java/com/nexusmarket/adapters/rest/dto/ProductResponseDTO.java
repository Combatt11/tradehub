package com.nexusmarket.adapters.rest.dto;

import com.nexusmarket.domain.models.ProductStatus;
import com.nexusmarket.domain.models.ProductType;

import java.util.List;
import java.util.UUID;

/**
 * Representacion publica de un producto.
 */
public record ProductResponseDTO(
        UUID id,
        UUID sellerId,
        String name,
        String description,
        ProductType type,
        ProductStatus status,
        List<VariantResponseDTO> variants
) {
}
