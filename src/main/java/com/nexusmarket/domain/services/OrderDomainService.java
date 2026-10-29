package com.nexusmarket.domain.services;

import com.nexusmarket.domain.models.Money;
import com.nexusmarket.domain.models.Order;
import com.nexusmarket.domain.models.OrderLine;
import com.nexusmarket.domain.models.OrderStatus;
import com.nexusmarket.domain.models.InvalidOrderStateException;
import com.nexusmarket.domain.ports.in.CreateOrderUseCase;
import com.nexusmarket.domain.ports.in.ProcessPaymentUseCase;
import com.nexusmarket.domain.ports.out.OrderRepositoryPort;
import com.nexusmarket.domain.ports.out.PaymentGatewayPort;
import com.nexusmarket.domain.ports.out.PaymentGatewayPort.PaymentResult;

import java.util.Currency;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;

/**
 * Servicio de dominio para el ciclo de creacion y pago de ordenes.
 *
 * <p>La inyeccion de dependencias se realiza exclusivamente por constructor;
 * no depende de Spring ni de frameworks de persistencia.</p>
 */
public final class OrderDomainService implements CreateOrderUseCase, ProcessPaymentUseCase {

    private final OrderRepositoryPort orderRepository;
    private final PaymentGatewayPort paymentGateway;

    public OrderDomainService(OrderRepositoryPort orderRepository, PaymentGatewayPort paymentGateway) {
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository must not be null");
        this.paymentGateway = Objects.requireNonNull(paymentGateway, "paymentGateway must not be null");
    }

    @Override
    public Order createOrder(UUID buyerId, UUID sellerId, List<OrderLine> lines) {
        Objects.requireNonNull(buyerId, "buyerId must not be null");
        Objects.requireNonNull(sellerId, "sellerId must not be null");
        Objects.requireNonNull(lines, "lines must not be null");

        if (lines.isEmpty()) {
            throw new IllegalArgumentException("An order must contain at least one line");
        }
        for (OrderLine line : lines) {
            Objects.requireNonNull(line, "lines must not contain null elements");
        }

        List<OrderLine> orderLines = List.copyOf(lines);
        Currency currency = orderLines.get(0).unitPrice().currency();
        Money total = Money.zero(currency);

        for (OrderLine line : orderLines) {
            if (!line.unitPrice().currency().equals(currency)) {
                throw new IllegalArgumentException(
                        "All order lines must use the same currency");
            }
            total = total.plus(line.lineTotal());
        }

        Order order = new Order(
                UUID.randomUUID(),
                buyerId,
                sellerId,
                orderLines,
                total,
                OrderStatus.PENDING_PAYMENT
        );

        return orderRepository.save(order);
    }

    @Override
    public PaymentResult processPayment(UUID orderId) {
        Objects.requireNonNull(orderId, "orderId must not be null");

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));

        OrderStatus currentStatus = order.getStatus();
        if (currentStatus != OrderStatus.PENDING_PAYMENT) {
            throw new InvalidOrderStateException(currentStatus, OrderStatus.PAID);
        }

        if (!order.getTotalAmount().isPositive()) {
            throw new IllegalStateException(
                    "Cannot process a payment for a non-positive order total");
        }

        PaymentResult result = Objects.requireNonNull(
                paymentGateway.charge(orderId, order.getTotalAmount()),
                "payment result must not be null");

        if (result.successful()) {
            order.markAsPaid();
        } else {
            order.markPaymentFailed();
        }

        orderRepository.save(order);
        return result;
    }
}
