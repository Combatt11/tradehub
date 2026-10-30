package com.nexusmarket.domain.ports.out;

import com.nexusmarket.domain.models.Product;

import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida para la persistencia de productos.
 */
public interface ProductRepositoryPort {

    Product save(Product product);

    Optional<Product> findById(UUID id);
}
