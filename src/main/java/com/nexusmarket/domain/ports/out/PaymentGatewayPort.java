package com.nexusmarket.domain.ports.out;

import com.nexusmarket.domain.models.Money;

import java.util.UUID;

/**
 * Puerto de salida para la pasarela de pago (PSP).
 *
 * <p>La implementacion concreta vivira en un adaptador de infraestructura.
 * En desarrollo/pruebas puede existir una pasarela simulada.</p>
 */
public interface PaymentGatewayPort {

    /**
     * Intenta cobrar el importe indicado.
     *
     * <p>{@code orderId} actua como clave de idempotencia: un reintento de red
     * con el mismo identificador no debe generar un segundo cobro.</p>
     *
     * @param orderId orden que origina el cobro
     * @param amount  importe exacto a cobrar
     * @return resultado de la operacion
     */
    PaymentResult charge(UUID orderId, Money amount);

    /**
     * Resultado devuelto por la pasarela.
     */
    record PaymentResult(boolean successful, String transactionId, String failureReason) {

        public PaymentResult {
            if (successful) {
                if (transactionId == null || transactionId.isBlank()) {
                    throw new IllegalArgumentException(
                            "transactionId is required for a successful payment");
                }
            } else if (failureReason == null || failureReason.isBlank()) {
                throw new IllegalArgumentException(
                        "failureReason is required for a failed payment");
            }
        }

        public static PaymentResult approved(String transactionId) {
            return new PaymentResult(true, transactionId, null);
        }

        public static PaymentResult declined(String failureReason) {
            return new PaymentResult(false, null, failureReason);
        }

        public boolean isSuccessful() {
            return successful;
        }
    }
}
