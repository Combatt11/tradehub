package com.nexusmarket.adapters.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Repositorio Spring Data JPA para almacenes.
 */
public interface JpaWarehouseRepository extends JpaRepository<WarehouseEntity, UUID> {
}
