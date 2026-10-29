package com.nexusmarket.adapters.rest.dto;

import com.nexusmarket.domain.models.OrderStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Representacion publica de una orden.
 */
public record OrderResponseDTO(
        UUID id,
        UUID buyerId,
        UUID sellerId,
        BigDecimal totalAmount,
        String currency,
        OrderStatus status,
        List<OrderLineResponseDTO> lines
) {
}
