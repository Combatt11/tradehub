package com.nexusmarket.domain.models;

import java.util.ArrayList;
import java.util.Currency;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Pedido: agregado raiz de fulfillment, facturacion, devolucion y liquidacion
 * de un vendedor.
 *
 * <p>Las lineas y el total solo pueden modificarse antes del pago. Los cambios
 * de estado se validan contra la maquina de estados de la seccion 6.</p>
 */
public final class Order {

    private final UUID id;
    private final UUID buyerId;
    private final UUID sellerId;
    private final List<OrderLine> lines;
    private Money totalAmount;
    private OrderStatus status;

    public Order(
            UUID id,
            UUID buyerId,
            UUID sellerId,
            List<OrderLine> lines,
            Money totalAmount,
            OrderStatus status
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.buyerId = Objects.requireNonNull(buyerId, "buyerId must not be null");
        this.sellerId = Objects.requireNonNull(sellerId, "sellerId must not be null");
        this.lines = new ArrayList<>(Objects.requireNonNull(lines, "lines must not be null"));
        this.totalAmount = Objects.requireNonNull(totalAmount, "totalAmount must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");

        validateLines(this.lines, this.totalAmount.currency());

        Money calculatedTotal = calculateTotal();
        if (calculatedTotal.compareTo(this.totalAmount) != 0) {
            throw new IllegalArgumentException(
                    "totalAmount " + this.totalAmount + " does not match lines total " + calculatedTotal);
        }
    }

    public static Order create(UUID id, UUID buyerId, UUID sellerId, Currency currency) {
        return new Order(
                id,
                buyerId,
                sellerId,
                List.of(),
                Money.zero(currency),
                OrderStatus.PENDING_PAYMENT
        );
    }

    public static Order create(UUID id, UUID buyerId, UUID sellerId, String currencyCode) {
        return create(id, buyerId, sellerId, Currency.getInstance(currencyCode));
    }

    /**
     * Anade una linea mientras el pedido aun no ha iniciado el pago.
     */
    public void addLine(OrderLine line) {
        Objects.requireNonNull(line, "line must not be null");
        ensureLinesAreMutable();

        if (!line.unitPrice().currency().equals(totalAmount.currency())) {
            throw new IllegalArgumentException(
                    "Line currency " + line.unitPrice().currency().getCurrencyCode()
                            + " does not match order currency "
                            + totalAmount.currency().getCurrencyCode());
        }
        if (lines.stream().anyMatch(existing -> existing.variantId().equals(line.variantId()))) {
            throw new IllegalArgumentException("Variant already exists in order: " + line.variantId());
        }

        lines.add(line);
        totalAmount = totalAmount.plus(line.lineTotal());
    }

    public void removeLine(UUID variantId) {
        Objects.requireNonNull(variantId, "variantId must not be null");
        ensureLinesAreMutable();

        boolean removed = lines.removeIf(line -> line.variantId().equals(variantId));
        if (!removed) {
            throw new IllegalArgumentException("Variant not found in order: " + variantId);
        }
        totalAmount = calculateTotal();
    }

    public void transitionTo(OrderStatus targetStatus) {
        Objects.requireNonNull(targetStatus, "targetStatus must not be null");
        if (!status.canTransitionTo(targetStatus)) {
            throw new InvalidOrderStateException(status, targetStatus);
        }
        this.status = targetStatus;
    }

    public void markPaymentFailed() {
        transitionTo(OrderStatus.PAYMENT_FAILED);
    }

    public void retryPayment() {
        transitionTo(OrderStatus.PENDING_PAYMENT);
    }

    public void markOnHold() {
        transitionTo(OrderStatus.ON_HOLD);
    }

    public void resumePayment() {
        transitionTo(OrderStatus.PENDING_PAYMENT);
    }

    public void resumeFulfillment() {
        transitionTo(OrderStatus.PAID);
    }

    public void markAsPaid() {
        if (lines.isEmpty()) {
            throw new IllegalStateException("Cannot mark an order as paid without lines");
        }
        transitionTo(OrderStatus.PAID);
    }

    public void startPreparing() {
        transitionTo(OrderStatus.PREPARING);
    }

    public void markAsDispatched() {
        transitionTo(OrderStatus.DISPATCHED);
    }

    public void markAsDelivered() {
        transitionTo(OrderStatus.DELIVERED);
    }

    public void finalizeOrder() {
        transitionTo(OrderStatus.FINALIZED);
    }

    public void cancel() {
        transitionTo(OrderStatus.CANCELLED);
    }

    public void requestReturn() {
        transitionTo(OrderStatus.RETURN_REQUESTED);
    }

    public void approveReturn() {
        transitionTo(OrderStatus.RETURN_IN_TRANSIT);
    }

    public void rejectReturn() {
        transitionTo(OrderStatus.DELIVERED);
    }

    public void receiveReturn() {
        transitionTo(OrderStatus.RETURN_RECEIVED);
    }

    public void acceptReturn() {
        transitionTo(OrderStatus.RETURNED);
    }

    public void rejectReturnInspection() {
        transitionTo(OrderStatus.DELIVERED);
    }

    public void requestRefund() {
        transitionTo(OrderStatus.REFUND_PENDING);
    }

    public void markAsRefunded() {
        transitionTo(OrderStatus.REFUNDED);
    }

    public boolean isTerminal() {
        return status.isTerminal();
    }

    public UUID getId() {
        return id;
    }

    public UUID getBuyerId() {
        return buyerId;
    }

    public UUID getSellerId() {
        return sellerId;
    }

    public List<OrderLine> getLines() {
        return List.copyOf(lines);
    }

    public Money getTotalAmount() {
        return totalAmount;
    }

    public OrderStatus getStatus() {
        return status;
    }

    private Money calculateTotal() {
        Money total = Money.zero(totalAmount.currency());
        for (OrderLine line : lines) {
            total = total.plus(line.lineTotal());
        }
        return total;
    }

    private void ensureLinesAreMutable() {
        if (status != OrderStatus.PENDING_PAYMENT) {
            throw new IllegalStateException(
                    "Order lines can only be modified while status is PENDING_PAYMENT");
        }
    }

    private static void validateLines(List<OrderLine> lines, Currency currency) {
        Set<UUID> variantIds = new HashSet<>();
        for (OrderLine line : lines) {
            if (line == null) {
                throw new IllegalArgumentException("lines must not contain null");
            }
            if (!line.unitPrice().currency().equals(currency)) {
                throw new IllegalArgumentException(
                        "Line currency " + line.unitPrice().currency().getCurrencyCode()
                                + " does not match order currency " + currency.getCurrencyCode());
            }
            if (!variantIds.add(line.variantId())) {
                throw new IllegalArgumentException("Duplicate variant in order: " + line.variantId());
            }
        }
    }
}
