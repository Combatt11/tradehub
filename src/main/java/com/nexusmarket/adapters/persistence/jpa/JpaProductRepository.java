package com.nexusmarket.adapters.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Repositorio Spring Data JPA para productos.
 */
public interface JpaProductRepository extends JpaRepository<ProductEntity, UUID> {
}
