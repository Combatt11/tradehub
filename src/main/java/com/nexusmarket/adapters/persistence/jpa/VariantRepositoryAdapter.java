package com.nexusmarket.adapters.persistence.jpa;

import com.nexusmarket.domain.models.Variant;
import com.nexusmarket.domain.ports.out.VariantRepositoryPort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador de salida para consultar variantes y sus precios.
 */
@Repository
public class VariantRepositoryAdapter implements VariantRepositoryPort {

    private final JpaVariantRepository variantRepository;

    public VariantRepositoryAdapter(JpaVariantRepository variantRepository) {
        this.variantRepository = Objects.requireNonNull(
                variantRepository, "variantRepository must not be null");
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Variant> findById(UUID variantId) {
        Objects.requireNonNull(variantId, "variantId must not be null");
        return variantRepository.findById(variantId)
                .map(entity -> ProductMapper.toDomain(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UUID> findSellerIdByVariantId(UUID variantId) {
        Objects.requireNonNull(variantId, "variantId must not be null");
        return variantRepository.findSellerIdByVariantId(variantId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Variant> findByIdAndSellerId(UUID variantId, UUID sellerId) {
        Objects.requireNonNull(variantId, "variantId must not be null");
        Objects.requireNonNull(sellerId, "sellerId must not be null");

        return variantRepository.findByIdAndSellerId(variantId, sellerId)
                .map(entity -> ProductMapper.toDomain(entity));
    }
}
