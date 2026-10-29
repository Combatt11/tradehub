package com.nexusmarket.domain.ports.in;

import java.util.List;
import java.util.UUID;

/**
 * Puerto de entrada para procesar el checkout completo de un comprador.
 *
 * <p>El checkout agrupa las variantes por vendedor, crea una orden por cada
 * vendedor y reserva el inventario correspondiente.</p>
 */
public interface CheckoutUseCase {

    /**
     * Procesa el carrito y devuelve los identificadores de las ordenes creadas.
     *
     * @param buyerId comprador propietario del carrito
     * @param items   variantes y cantidades del carrito
     * @return IDs de las ordenes creadas, una por vendedor
     */
    List<UUID> checkout(UUID buyerId, List<CheckoutItemDTO> items);
}
