package com.nexusmarket.adapters.payment;

import com.nexusmarket.domain.models.Money;
import com.nexusmarket.domain.ports.out.PaymentGatewayPort;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Pasarela de pago simulada para desarrollo y pruebas.
 *
 * <p>Aprueba aproximadamente el 90% de los cobros y rechaza el 10%
 * restante. No realiza ninguna llamada externa.</p>
 */
@Component
public class SimulatedPaymentGatewayAdapter implements PaymentGatewayPort {

    private static final double SUCCESS_PROBABILITY = 0.90;

    @Override
    public PaymentResult charge(UUID orderId, Money amount) {
        Objects.requireNonNull(orderId, "orderId must not be null");
        Objects.requireNonNull(amount, "amount must not be null");

        if (ThreadLocalRandom.current().nextDouble() < SUCCESS_PROBABILITY) {
            return PaymentResult.approved("SIM-" + UUID.randomUUID());
        }

        return PaymentResult.declined("SIMULATED_PAYMENT_DECLINED");
    }
}
