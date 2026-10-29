package com.nexusmarket.infrastructure.config;

import com.nexusmarket.domain.ports.out.InventoryRepositoryPort;
import com.nexusmarket.domain.ports.out.OrderRepositoryPort;
import com.nexusmarket.domain.ports.out.PaymentGatewayPort;
import com.nexusmarket.domain.services.InventoryDomainService;
import com.nexusmarket.domain.services.OrderDomainService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuracion de los servicios de dominio puros como beans de Spring.
 *
 * <p>Los servicios de dominio no llevan anotaciones de Spring; se construyen
 * aqui mediante inyeccion por constructor.</p>
 */
@Configuration
public class BeanConfiguration {

    @Bean
    public OrderDomainService orderDomainService(
            OrderRepositoryPort orderRepositoryPort,
            PaymentGatewayPort paymentGatewayPort
    ) {
        return new OrderDomainService(orderRepositoryPort, paymentGatewayPort);
    }

    @Bean
    public InventoryDomainService inventoryDomainService(
            InventoryRepositoryPort inventoryRepositoryPort
    ) {
        return new InventoryDomainService(inventoryRepositoryPort);
    }
}
