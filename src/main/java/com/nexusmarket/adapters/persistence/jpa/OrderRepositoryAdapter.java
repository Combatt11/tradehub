package com.nexusmarket.adapters.persistence.jpa;

import com.nexusmarket.domain.models.Order;
import com.nexusmarket.domain.ports.out.OrderRepositoryPort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador de salida que implementa {@link OrderRepositoryPort} mediante JPA.
 */
@Repository
public class OrderRepositoryAdapter implements OrderRepositoryPort {

    private final JpaOrderRepository orderRepository;

    public OrderRepositoryAdapter(JpaOrderRepository orderRepository) {
        this.orderRepository = Objects.requireNonNull(
                orderRepository, "orderRepository must not be null");
    }

    @Override
    @Transactional
    public Order save(Order order) {
        Objects.requireNonNull(order, "order must not be null");

        OrderEntity entity = orderRepository.findById(order.getId())
                .map(existing -> {
                    OrderMapper.updateEntity(existing, order);
                    return existing;
                })
                .orElseGet(() -> OrderMapper.toEntity(order));

        return OrderMapper.toDomain(orderRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Order> findById(UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        return orderRepository.findById(id).map(OrderMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> findAllByCreatedAtBetween(LocalDateTime start, LocalDateTime end) {
        Objects.requireNonNull(start, "start must not be null");
        Objects.requireNonNull(end, "end must not be null");

        return orderRepository.findAllByCreatedAtBetween(start, end).stream()
                .map(OrderMapper::toDomain)
                .toList();
    }
}
