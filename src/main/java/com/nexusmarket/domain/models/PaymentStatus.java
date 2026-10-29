package com.nexusmarket.domain.models;

/**
 * Estados de la sub-maquina de pago.
 *
 * <p>Un pago FAILED no se reutiliza: se crea uno nuevo. El estado del pedido
 * solo pasa a REFUNDED cuando el PSP confirma el reembolso total.</p>
 */
public enum PaymentStatus {
    CREATED,
    REQUIRES_ACTION,
    UNKNOWN,
    AUTHORIZED,
    CAPTURED,
    FAILED,
    CANCELLED,
    PARTIALLY_REFUNDED,
    REFUNDED,
    DISPUTED;

    public boolean canTransitionTo(PaymentStatus target) {
        if (target == null) {
            return false;
        }

        return switch (this) {
            case CREATED -> target == REQUIRES_ACTION
                    || target == AUTHORIZED
                    || target == CAPTURED
                    || target == FAILED
                    || target == CANCELLED
                    || target == UNKNOWN;
            case REQUIRES_ACTION -> target == AUTHORIZED
                    || target == CAPTURED
                    || target == FAILED
                    || target == CANCELLED
                    || target == UNKNOWN;
            case AUTHORIZED -> target == CAPTURED
                    || target == FAILED
                    || target == CANCELLED
                    || target == UNKNOWN;
            case UNKNOWN -> target == CAPTURED
                    || target == FAILED
                    || target == CANCELLED;
            case CAPTURED -> target == PARTIALLY_REFUNDED
                    || target == REFUNDED
                    || target == DISPUTED;
            case PARTIALLY_REFUNDED -> target == REFUNDED
                    || target == DISPUTED;
            case DISPUTED -> target == CAPTURED
                    || target == REFUNDED;
            case FAILED, CANCELLED, REFUNDED -> false;
        };
    }

    public boolean isTerminal() {
        return this == FAILED || this == CANCELLED || this == REFUNDED;
    }
}
