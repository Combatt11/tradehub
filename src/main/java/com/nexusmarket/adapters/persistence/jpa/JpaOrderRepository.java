package com.nexusmarket.adapters.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repositorio Spring Data JPA para pedidos.
 */
public interface JpaOrderRepository extends JpaRepository<OrderEntity, UUID> {

    List<OrderEntity> findAllByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}
