package com.nexusmarket.domain.models;

/**
 * Se lanza cuando se intenta una transicion no permitida en la maquina de
 * estados del pedido.
 */
public final class InvalidOrderStateException extends RuntimeException {

    private final OrderStatus currentStatus;
    private final OrderStatus targetStatus;

    public InvalidOrderStateException(OrderStatus currentStatus, OrderStatus targetStatus) {
        super("Invalid order transition: " + currentStatus + " -> " + targetStatus);
        this.currentStatus = currentStatus;
        this.targetStatus = targetStatus;
    }

    public OrderStatus getCurrentStatus() {
        return currentStatus;
    }

    public OrderStatus getTargetStatus() {
        return targetStatus;
    }
}
