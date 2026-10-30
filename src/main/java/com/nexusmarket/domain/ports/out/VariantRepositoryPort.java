package com.nexusmarket.domain.ports.out;

import com.nexusmarket.domain.models.Variant;

import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida para consultar variantes y sus precios autoritativos.
 */
public interface VariantRepositoryPort {

    Optional<Variant> findById(UUID variantId);

    /**
     * Obtiene el vendedor propietario de una variante.
     */
    Optional<UUID> findSellerIdByVariantId(UUID variantId);

    /**
     * Busca una variante asegurando que pertenece al vendedor indicado.
     */
    Optional<Variant> findByIdAndSellerId(UUID variantId, UUID sellerId);
}
