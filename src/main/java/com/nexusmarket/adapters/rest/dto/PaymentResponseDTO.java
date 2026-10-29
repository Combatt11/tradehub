package com.nexusmarket.adapters.rest.dto;

/**
 * Resultado publico del procesamiento de un pago.
 */
public record PaymentResponseDTO(
        boolean successful,
        String transactionId,
        String failureReason
) {
}
