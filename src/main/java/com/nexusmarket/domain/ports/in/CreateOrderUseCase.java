package com.nexusmarket.domain.ports.in;

import com.nexusmarket.domain.models.Order;
import com.nexusmarket.domain.models.OrderLine;

import java.util.List;
import java.util.UUID;

/**
 * Puerto de entrada para crear una orden a partir de las lineas de un carrito
 * previamente validado.
 */
public interface CreateOrderUseCase {

    /**
     * Crea una orden en estado PENDING_PAYMENT y calcula su total.
     *
     * @param buyerId  comprador propietario de la compra
     * @param sellerId vendedor al que pertenece la orden
     * @param lines    lineas del carrito con precio congelado
     * @return orden persistida
     */
    Order createOrder(UUID buyerId, UUID sellerId, List<OrderLine> lines);
}
