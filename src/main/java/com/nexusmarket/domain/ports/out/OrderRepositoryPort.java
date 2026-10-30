package com.nexusmarket.domain.ports.out;

import com.nexusmarket.domain.models.Order;

import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida para la persistencia de ordenes.
 */
public interface OrderRepositoryPort {

    Order save(Order order);

    Optional<Order> findById(UUID id);
}
