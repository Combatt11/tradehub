package com.nexusmarket.domain.services;

import com.nexusmarket.domain.models.InvalidOrderStateException;
import com.nexusmarket.domain.models.Money;
import com.nexusmarket.domain.models.Order;
import com.nexusmarket.domain.models.OrderLine;
import com.nexusmarket.domain.models.OrderStatus;
import com.nexusmarket.domain.models.UnauthorizedOperationException;
import com.nexusmarket.domain.models.Variant;
import com.nexusmarket.domain.ports.in.CreateOrderUseCase.OrderLineInput;
import com.nexusmarket.domain.ports.out.OrderRepositoryPort;
import com.nexusmarket.domain.ports.out.PaymentGatewayPort;
import com.nexusmarket.domain.ports.out.PaymentGatewayPort.PaymentResult;
import com.nexusmarket.domain.ports.out.VariantRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderDomainServiceTest {

    @Mock
    private OrderRepositoryPort orderRepository;

    @Mock
    private PaymentGatewayPort paymentGateway;

    @Mock
    private VariantRepositoryPort variantRepository;

    private OrderDomainService orderDomainService;

    private final UUID buyerId = UUID.randomUUID();
    private final UUID sellerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        orderDomainService = new OrderDomainService(
                orderRepository,
                paymentGateway,
                variantRepository
        );
    }

    @Test
    void createOrderCalculatesTotalUsingServerPrices() {
        UUID firstVariantId = UUID.randomUUID();
        UUID secondVariantId = UUID.randomUUID();

        when(variantRepository.findByIdAndSellerId(firstVariantId, sellerId))
                .thenReturn(Optional.of(new Variant(
                        firstVariantId,
                        "SKU-001",
                        Money.of("10.50", "USD")
                )));
        when(variantRepository.findByIdAndSellerId(secondVariantId, sellerId))
                .thenReturn(Optional.of(new Variant(
                        secondVariantId,
                        "SKU-002",
                        Money.of("20.00", "USD")
                )));
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Order order = orderDomainService.createOrder(
                buyerId,
                sellerId,
                List.of(
                        new OrderLineInput(firstVariantId, 2),
                        new OrderLineInput(secondVariantId, 3)
                )
        );

        assertEquals(Money.of("81.00", "USD"), order.getTotalAmount());
        assertEquals(2, order.getLines().size());
        assertEquals(OrderStatus.PENDING_PAYMENT, order.getStatus());
        verify(orderRepository).save(order);
    }

    @Test
    void processPaymentThrowsWhenBuyerDoesNotOwnOrder() {
        UUID orderId = UUID.randomUUID();
        UUID anotherBuyerId = UUID.randomUUID();
        Order order = orderWithLine(orderId, buyerId, "10.00");

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThrows(
                UnauthorizedOperationException.class,
                () -> orderDomainService.processPayment(orderId, anotherBuyerId)
        );

        verifyNoInteractions(paymentGateway);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void processPaymentMarksOrderAsPaidWhenGatewayApproves() {
        UUID orderId = UUID.randomUUID();
        Order order = orderWithLine(orderId, buyerId, "10.00");
        PaymentResult paymentResult = PaymentResult.approved("tx-123");

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(paymentGateway.charge(orderId, order.getTotalAmount()))
                .thenReturn(paymentResult);

        PaymentResult result = orderDomainService.processPayment(orderId, buyerId);

        assertSame(paymentResult, result);
        assertEquals(OrderStatus.PAID, order.getStatus());
        verify(paymentGateway).charge(orderId, order.getTotalAmount());
        verify(orderRepository).save(order);
    }

    @Test
    void processPaymentMarksAsFailedWhenGatewayDeclines() {
        UUID orderId = UUID.randomUUID();
        Order order = orderWithLine(orderId, buyerId, "10.00");
        PaymentResult paymentResult = PaymentResult.declined("INSUFFICIENT_FUNDS");

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(paymentGateway.charge(orderId, order.getTotalAmount()))
                .thenReturn(paymentResult);

        PaymentResult result = orderDomainService.processPayment(orderId, buyerId);

        assertSame(paymentResult, result);
        assertEquals(OrderStatus.PAYMENT_FAILED, order.getStatus());
        verify(paymentGateway).charge(orderId, order.getTotalAmount());
        verify(orderRepository).save(order);
    }

    @Test
    void processPaymentThrowsWhenOrderIsCancelled() {
        UUID orderId = UUID.randomUUID();
        Order order = orderWithLine(orderId, buyerId, "10.00", OrderStatus.CANCELLED);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThrows(
                InvalidOrderStateException.class,
                () -> orderDomainService.processPayment(orderId, buyerId)
        );

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        verifyNoInteractions(paymentGateway);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void processPaymentThrowsWhenOrderIsAlreadyPaid() {
        UUID orderId = UUID.randomUUID();
        Order order = orderWithLine(orderId, buyerId, "10.00", OrderStatus.PAID);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThrows(
                InvalidOrderStateException.class,
                () -> orderDomainService.processPayment(orderId, buyerId)
        );

        assertEquals(OrderStatus.PAID, order.getStatus());
        verifyNoInteractions(paymentGateway);
        verify(orderRepository, never()).save(any(Order.class));
    }

    private Order orderWithLine(UUID orderId, UUID orderBuyerId, String unitPrice) {
        return orderWithLine(orderId, orderBuyerId, unitPrice, OrderStatus.PENDING_PAYMENT);
    }

    private Order orderWithLine(
            UUID orderId,
            UUID orderBuyerId,
            String unitPrice,
            OrderStatus status
    ) {
        Money price = Money.of(unitPrice, "USD");
        UUID variantId = UUID.randomUUID();

        return new Order(
                orderId,
                orderBuyerId,
                sellerId,
                List.of(new OrderLine(variantId, 1, price)),
                price,
                status
        );
    }
}
