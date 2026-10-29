package com.nexusmarket.adapters.persistence.jpa;

import com.nexusmarket.domain.models.Product;
import com.nexusmarket.domain.ports.out.ProductRepositoryPort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador de salida que implementa {@link ProductRepositoryPort} mediante JPA.
 */
@Repository
public class ProductRepositoryAdapter implements ProductRepositoryPort {

    private final JpaProductRepository productRepository;

    public ProductRepositoryAdapter(JpaProductRepository productRepository) {
        this.productRepository = Objects.requireNonNull(
                productRepository, "productRepository must not be null");
    }

    @Override
    @Transactional
    public Product save(Product product) {
        Objects.requireNonNull(product, "product must not be null");

        ProductEntity entity = productRepository.findById(product.getId())
                .map(existing -> {
                    ProductMapper.updateEntity(existing, product);
                    return existing;
                })
                .orElseGet(() -> ProductMapper.toEntity(product));

        return ProductMapper.toDomain(productRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Product> findById(UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        return productRepository.findById(id).map(ProductMapper::toDomain);
    }
}
