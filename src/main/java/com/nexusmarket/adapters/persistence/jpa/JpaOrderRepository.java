package com.nexusmarket.adapters.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Repositorio Spring Data JPA para pedidos.
 */
public interface JpaOrderRepository extends JpaRepository<OrderEntity, UUID> {
}
