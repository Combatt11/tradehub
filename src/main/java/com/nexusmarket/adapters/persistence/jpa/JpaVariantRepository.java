package com.nexusmarket.adapters.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio Spring Data JPA para variantes/SKU.
 */
public interface JpaVariantRepository extends JpaRepository<VariantEntity, UUID> {

    @Query("""
            select variant
            from VariantEntity variant
            where variant.id = :variantId
              and variant.product.sellerId = :sellerId
            """)
    Optional<VariantEntity> findByIdAndSellerId(
            @Param("variantId") UUID variantId,
            @Param("sellerId") UUID sellerId
    );

    @Query("""
            select variant.product.sellerId
            from VariantEntity variant
            where variant.id = :variantId
            """)
    Optional<UUID> findSellerIdByVariantId(@Param("variantId") UUID variantId);
}
