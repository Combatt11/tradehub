package com.nexusmarket.adapters.rest.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Variante/SKU recibida al crear un producto.
 *
 * <p>El {@code id} es opcional: si no se envia, el servidor genera uno nuevo.</p>
 */
public record VariantRequestDTO(
        UUID id,

        @NotBlank(message = "sku is required")
        @Size(max = 64, message = "sku must not exceed 64 characters")
        String sku,

        @NotNull(message = "price is required")
        @DecimalMin(value = "0.01", message = "price must be greater than zero")
        BigDecimal price,

        @NotBlank(message = "currency is required")
        @Pattern(regexp = "[A-Z]{3}", message = "currency must be a 3-letter ISO code")
        String currency,

        Map<String, String> attributes
) {
}
