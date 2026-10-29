package com.nexusmarket.domain.ports.in;

import com.nexusmarket.domain.ports.out.PaymentGatewayPort.PaymentResult;

import java.util.UUID;

/**
 * Puerto de entrada para procesar el pago de una orden.
 */
public interface ProcessPaymentUseCase {

    /**
     * Procesa el pago de una orden, actualiza su estado y la persiste.
     *
     * @param orderId identificador de la orden
     * @return resultado de la pasarela
     */
    PaymentResult processPayment(UUID orderId);
}
