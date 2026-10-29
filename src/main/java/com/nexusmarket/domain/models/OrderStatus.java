package com.nexusmarket.domain.models;

/**
 * Estados del ciclo comercial y de fulfillment de un pedido.
 *
 * <p>Las transiciones permitidas se basan en la maquina de estados de la
 * seccion 6 de la especificacion. Los estados financieros, de envio, de
 * devolucion y de derecho digital se gestionan en sub-maquinas separadas.</p>
 */
public enum OrderStatus {
    PENDING_PAYMENT,
    PAYMENT_FAILED,
    ON_HOLD,
    PAID,
    PREPARING,
    DISPATCHED,
    DELIVERED,
    FINALIZED,
    CANCELLED,
    RETURN_REQUESTED,
    RETURN_IN_TRANSIT,
    RETURN_RECEIVED,
    RETURNED,
    REFUND_PENDING,
    REFUNDED;

    public boolean canTransitionTo(OrderStatus target) {
        if (target == null) {
            return false;
        }

        return switch (this) {
            case PENDING_PAYMENT -> target == PAYMENT_FAILED
                    || target == ON_HOLD
                    || target == PAID
                    || target == CANCELLED;
            case PAYMENT_FAILED -> target == PENDING_PAYMENT
                    || target == CANCELLED;
            case ON_HOLD -> target == PENDING_PAYMENT
                    || target == PAID
                    || target == CANCELLED;
            case PAID -> target == ON_HOLD
                    || target == PREPARING
                    || target == DELIVERED
                    || target == CANCELLED;
            case PREPARING -> target == DISPATCHED
                    || target == CANCELLED;
            case DISPATCHED -> target == DELIVERED
                    || target == CANCELLED;
            case DELIVERED -> target == FINALIZED
                    || target == RETURN_REQUESTED
                    || target == REFUND_PENDING;
            case RETURN_REQUESTED -> target == RETURN_IN_TRANSIT
                    || target == DELIVERED;
            case RETURN_IN_TRANSIT -> target == RETURN_RECEIVED;
            case RETURN_RECEIVED -> target == RETURNED
                    || target == DELIVERED;
            case RETURNED -> target == REFUND_PENDING;
            case CANCELLED -> target == REFUND_PENDING;
            case REFUND_PENDING -> target == REFUNDED;
            case FINALIZED, REFUNDED -> false;
        };
    }

    /**
     * FINALIZED y REFUNDED son terminales. CANCELLED puede continuar a
     * reembolso si hubo captura, por lo que no se considera terminal.
     */
    public boolean isTerminal() {
        return this == FINALIZED || this == REFUNDED;
    }
}
