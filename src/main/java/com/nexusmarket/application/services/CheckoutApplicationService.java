package com.nexusmarket.application.services;

import com.nexusmarket.domain.models.Order;
import com.nexusmarket.domain.ports.in.CheckoutItemDTO;
import com.nexusmarket.domain.ports.in.CheckoutUseCase;
import com.nexusmarket.domain.ports.in.CreateOrderUseCase;
import com.nexusmarket.domain.ports.in.CreateOrderUseCase.OrderLineInput;
import com.nexusmarket.domain.ports.in.ReserveInventoryUseCase;
import com.nexusmarket.domain.ports.out.InventoryRepositoryPort;
import com.nexusmarket.domain.ports.out.OrderRepositoryPort;
import com.nexusmarket.domain.ports.out.VariantRepositoryPort;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;

/**
 * Orquestador del checkout multi-vendedor.
 *
 * <p>Agrupa las variantes por vendedor, crea una orden por vendedor y reserva
 * el inventario de cada linea. Si algo falla, compensa las reservas y ordenes
 * ya creadas.</p>
 */
public final class CheckoutApplicationService implements CheckoutUseCase {

    private final CreateOrderUseCase createOrderUseCase;
    private final ReserveInventoryUseCase reserveInventoryUseCase;
    private final VariantRepositoryPort variantRepository;
    private final OrderRepositoryPort orderRepository;
    private final InventoryRepositoryPort inventoryRepository;
    private final UUID defaultWarehouseId;

    public CheckoutApplicationService(
            CreateOrderUseCase createOrderUseCase,
            ReserveInventoryUseCase reserveInventoryUseCase,
            VariantRepositoryPort variantRepository,
            OrderRepositoryPort orderRepository,
            InventoryRepositoryPort inventoryRepository,
            UUID defaultWarehouseId
    ) {
        this.createOrderUseCase = Objects.requireNonNull(
                createOrderUseCase, "createOrderUseCase must not be null");
        this.reserveInventoryUseCase = Objects.requireNonNull(
                reserveInventoryUseCase, "reserveInventoryUseCase must not be null");
        this.variantRepository = Objects.requireNonNull(
                variantRepository, "variantRepository must not be null");
        this.orderRepository = Objects.requireNonNull(
                orderRepository, "orderRepository must not be null");
        this.inventoryRepository = Objects.requireNonNull(
                inventoryRepository, "inventoryRepository must not be null");
        this.defaultWarehouseId = Objects.requireNonNull(
                defaultWarehouseId, "defaultWarehouseId must not be null");
    }

    @Override
    public List<UUID> checkout(UUID buyerId, List<CheckoutItemDTO> items) {
        Objects.requireNonNull(buyerId, "buyerId must not be null");
        Objects.requireNonNull(items, "items must not be null");

        if (items.isEmpty()) {
            throw new IllegalArgumentException("Checkout must contain at least one item");
        }
        for (CheckoutItemDTO item : items) {
            Objects.requireNonNull(item, "items must not contain null elements");
        }

        Map<UUID, List<OrderLineInput>> itemsBySeller = groupBySeller(items);
        List<UUID> createdOrderIds = new ArrayList<>();
        List<Reservation> reservations = new ArrayList<>();

        try {
            for (Map.Entry<UUID, List<OrderLineInput>> entry : itemsBySeller.entrySet()) {
                UUID sellerId = entry.getKey();
                List<OrderLineInput> sellerItems = entry.getValue();

                Order order = Objects.requireNonNull(
                        createOrderUseCase.createOrder(buyerId, sellerId, sellerItems),
                        "created order must not be null");
                createdOrderIds.add(order.getId());

                for (OrderLineInput item : sellerItems) {
                    reserveInventoryUseCase.reserveStock(
                            item.variantId(),
                            defaultWarehouseId,
                            item.quantity()
                    );
                    reservations.add(new Reservation(item.variantId(), item.quantity()));
                }
            }

            return List.copyOf(createdOrderIds);
        } catch (RuntimeException exception) {
            rollbackReservations(reservations, exception);
            rollbackOrders(createdOrderIds, exception);
            throw exception;
        }
    }

    private Map<UUID, List<OrderLineInput>> groupBySeller(List<CheckoutItemDTO> items) {
        Map<UUID, List<OrderLineInput>> grouped = new LinkedHashMap<>();

        for (CheckoutItemDTO item : items) {
            UUID sellerId = variantRepository.findSellerIdByVariantId(item.variantId())
                    .orElseThrow(() -> new NoSuchElementException(
                            "Variant not found: " + item.variantId()));

            grouped.computeIfAbsent(sellerId, ignored -> new ArrayList<>())
                    .add(new OrderLineInput(item.variantId(), item.quantity()));
        }

        return grouped;
    }

    private void rollbackReservations(
            List<Reservation> reservations,
            RuntimeException originalException
    ) {
        for (Reservation reservation : reservations) {
            try {
                inventoryRepository.findByVariantAndWarehouse(
                                reservation.variantId(),
                                defaultWarehouseId)
                        .ifPresent(item -> {
                            item.releaseReservation(reservation.quantity());
                            inventoryRepository.save(item);
                        });
            } catch (RuntimeException rollbackFailure) {
                originalException.addSuppressed(rollbackFailure);
            }
        }
    }

    private void rollbackOrders(List<UUID> orderIds, RuntimeException originalException) {
        for (UUID orderId : orderIds) {
            try {
                orderRepository.findById(orderId).ifPresent(order -> {
                    order.cancel();
                    orderRepository.save(order);
                });
            } catch (RuntimeException rollbackFailure) {
                originalException.addSuppressed(rollbackFailure);
            }
        }
    }

    private record Reservation(UUID variantId, int quantity) {
    }
}
