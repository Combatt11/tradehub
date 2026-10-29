package com.nexusmarket.adapters.rest.dto;

import java.util.List;
import java.util.UUID;

/**
 * Resultado del checkout con las ordenes creadas.
 */
public record CheckoutResponseDTO(List<UUID> orderIds) {
}
