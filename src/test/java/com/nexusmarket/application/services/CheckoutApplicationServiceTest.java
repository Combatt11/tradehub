package com.nexusmarket.application.services;

import com.nexusmarket.domain.models.CartItem;
import com.nexusmarket.domain.models.InsufficientStockException;
import com.nexusmarket.domain.models.Money;
import com.nexusmarket.domain.models.Order;
import com.nexusmarket.domain.models.OrderLine;
import com.nexusmarket.domain.models.OrderStatus;
import com.nexusmarket.domain.models.ShoppingCart;
import com.nexusmarket.domain.ports.in.CreateOrderUseCase;
import com.nexusmarket.domain.ports.in.ReserveInventoryUseCase;
import com.nexusmarket.domain.ports.out.InventoryRepositoryPort;
import com.nexusmarket.domain.ports.out.OrderRepositoryPort;
import com.nexusmarket.domain.ports.out.ShoppingCartRepositoryPort;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckoutApplicationServiceTest {

    private static final UUID DEFAULT_WAREHOUSE_ID = UUID.randomUUID();

    @Mock
    private CreateOrderUseCase createOrderUseCase;

    @Mock
    private ReserveInventoryUseCase reserveInventoryUseCase;

    @Mock
    private VariantRepositoryPort variantRepository;

    @Mock
    private OrderRepositoryPort orderRepository;

    @Mock
    private InventoryRepositoryPort inventoryRepository;

    @Mock
    private ShoppingCartRepositoryPort shoppingCartRepository;

    private CheckoutApplicationService checkoutApplicationService;

    @BeforeEach
    void setUp() {
        checkoutApplicationService = new CheckoutApplicationService(
                createOrderUseCase,
                reserveInventoryUseCase,
                variantRepository,
                orderRepository,
                inventoryRepository,
                shoppingCartRepository,
                DEFAULT_WAREHOUSE_ID
        );
    }

    @Test
    void checkoutCreatesOneOrderPerSeller() {
        UUID buyerId = UUID.randomUUID();
        UUID firstSellerId = UUID.randomUUID();
        UUID secondSellerId = UUID.randomUUID();
        UUID firstVariantId = UUID.randomUUID();
        UUID secondVariantId = UUID.randomUUID();

        ShoppingCart cart = new ShoppingCart(
                UUID.randomUUID(),
                buyerId,
                List.of(
                        new CartItem(firstVariantId, 1),
                        new CartItem(secondVariantId, 2)
                )
        );

        Order firstOrder = orderFor(
                buyerId,
                firstSellerId,
                firstVariantId,
                1,
                "10.00"
        );
        Order secondOrder = orderFor(
                buyerId,
                secondSellerId,
                secondVariantId,
                2,
                "5.00"
        );

        when(shoppingCartRepository.findByBuyerId(buyerId))
                .thenReturn(Optional.of(cart));
        when(variantRepository.findSellerIdByVariantId(firstVariantId))
                .thenReturn(Optional.of(firstSellerId));
        when(variantRepository.findSellerIdByVariantId(secondVariantId))
                .thenReturn(Optional.of(secondSellerId));
        when(createOrderUseCase.createOrder(eq(buyerId), eq(firstSellerId), anyList()))
                .thenReturn(firstOrder);
        when(createOrderUseCase.createOrder(eq(buyerId), eq(secondSellerId), anyList()))
                .thenReturn(secondOrder);

        List<UUID> orderIds = checkoutApplicationService.checkout(buyerId);

        assertEquals(List.of(firstOrder.getId(), secondOrder.getId()), orderIds);
        verify(createOrderUseCase).createOrder(eq(buyerId), eq(firstSellerId), anyList());
        verify(createOrderUseCase).createOrder(eq(buyerId), eq(secondSellerId), anyList());
        verify(createOrderUseCase, times(2)).createOrder(eq(buyerId), any(), anyList());
        verify(reserveInventoryUseCase, times(2))
                .reserveStock(any(UUID.class), eq(DEFAULT_WAREHOUSE_ID), anyInt());
        verify(shoppingCartRepository).deleteByBuyerId(buyerId);
    }

    @Test
    void checkoutRollsBackWhenOneSellerFails() {
        UUID buyerId = UUID.randomUUID();
        UUID firstSellerId = UUID.randomUUID();
        UUID secondSellerId = UUID.randomUUID();
        UUID firstVariantId = UUID.randomUUID();
        UUID secondVariantId = UUID.randomUUID();
        UUID firstOrderId = UUID.randomUUID();
        UUID secondOrderId = UUID.randomUUID();

        ShoppingCart cart = new ShoppingCart(
                UUID.randomUUID(),
                buyerId,
                List.of(
                        new CartItem(firstVariantId, 1),
                        new CartItem(secondVariantId, 2)
                )
        );

        Order firstOrder = mock(Order.class);
        Order secondOrder = mock(Order.class);
        when(firstOrder.getId()).thenReturn(firstOrderId);
        when(secondOrder.getId()).thenReturn(secondOrderId);

        when(shoppingCartRepository.findByBuyerId(buyerId))
                .thenReturn(Optional.of(cart));
        when(variantRepository.findSellerIdByVariantId(firstVariantId))
                .thenReturn(Optional.of(firstSellerId));
        when(variantRepository.findSellerIdByVariantId(secondVariantId))
                .thenReturn(Optional.of(secondSellerId));
        when(createOrderUseCase.createOrder(eq(buyerId), eq(firstSellerId), anyList()))
                .thenReturn(firstOrder);
        when(createOrderUseCase.createOrder(eq(buyerId), eq(secondSellerId), anyList()))
                .thenReturn(secondOrder);
        when(reserveInventoryUseCase.reserveStock(
                eq(firstVariantId),
                eq(DEFAULT_WAREHOUSE_ID),
                eq(1)
        )).thenReturn(null);
        when(reserveInventoryUseCase.reserveStock(
                eq(secondVariantId),
                eq(DEFAULT_WAREHOUSE_ID),
                eq(2)
        )).thenThrow(new InsufficientStockException(
                secondVariantId,
                DEFAULT_WAREHOUSE_ID,
                1,
                2
        ));
        when(orderRepository.findById(firstOrderId))
                .thenReturn(Optional.of(firstOrder));

        assertThrows(
                InsufficientStockException.class,
                () -> checkoutApplicationService.checkout(buyerId)
        );

        verify(orderRepository).findById(firstOrderId);
        verify(firstOrder).cancel();
        verify(orderRepository).save(firstOrder);
        verify(shoppingCartRepository, never()).deleteByBuyerId(any(UUID.class));
    }

    @Test
    void checkoutThrowsWhenCartIsEmpty() {
        UUID buyerId = UUID.randomUUID();
        ShoppingCart emptyCart = ShoppingCart.create(UUID.randomUUID(), buyerId);

        when(shoppingCartRepository.findByBuyerId(buyerId))
                .thenReturn(Optional.of(emptyCart));

        assertThrows(
                IllegalStateException.class,
                () -> checkoutApplicationService.checkout(buyerId)
        );

        verifyNoInteractions(createOrderUseCase, reserveInventoryUseCase);
        verify(shoppingCartRepository, never()).deleteByBuyerId(any(UUID.class));
    }

    private Order orderFor(
            UUID buyerId,
            UUID sellerId,
            UUID variantId,
            int quantity,
            String unitPrice
    ) {
        Money price = Money.of(unitPrice, "USD");

        return new Order(
                UUID.randomUUID(),
                buyerId,
                sellerId,
                List.of(new OrderLine(variantId, quantity, price)),
                price.multiply(quantity),
                OrderStatus.PENDING_PAYMENT
        );
    }
}
