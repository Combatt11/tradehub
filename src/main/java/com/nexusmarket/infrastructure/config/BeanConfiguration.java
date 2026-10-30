package com.nexusmarket.infrastructure.config;

import com.nexusmarket.application.services.AuthApplicationService;
import com.nexusmarket.application.services.CheckoutApplicationService;
import com.nexusmarket.application.services.ProductApplicationService;
import com.nexusmarket.application.services.ReportApplicationService;
import com.nexusmarket.domain.ports.in.CheckoutUseCase;
import com.nexusmarket.domain.ports.in.CreateOrderUseCase;
import com.nexusmarket.domain.ports.in.GetSalesReportUseCase;
import com.nexusmarket.domain.ports.in.ManageCartUseCase;
import com.nexusmarket.domain.ports.in.ManageProductUseCase;
import com.nexusmarket.domain.ports.in.ReserveInventoryUseCase;
import com.nexusmarket.domain.ports.out.InventoryRepositoryPort;
import com.nexusmarket.domain.ports.out.JwtTokenPort;
import com.nexusmarket.domain.ports.out.OrderRepositoryPort;
import com.nexusmarket.domain.ports.out.PasswordHasherPort;
import com.nexusmarket.domain.ports.out.PaymentGatewayPort;
import com.nexusmarket.domain.ports.out.ProductRepositoryPort;
import com.nexusmarket.domain.ports.out.ShoppingCartRepositoryPort;
import com.nexusmarket.domain.ports.out.UserRepositoryPort;
import com.nexusmarket.domain.ports.out.VariantRepositoryPort;
import com.nexusmarket.domain.services.CartDomainService;
import com.nexusmarket.domain.services.InventoryDomainService;
import com.nexusmarket.domain.services.OrderDomainService;
import com.nexusmarket.infrastructure.seed.DatabaseSeeder;
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
            PaymentGatewayPort paymentGatewayPort,
            VariantRepositoryPort variantRepositoryPort
    ) {
        return new OrderDomainService(
                orderRepositoryPort,
                paymentGatewayPort,
                variantRepositoryPort
        );
    }

    @Bean
    public InventoryDomainService inventoryDomainService(
            InventoryRepositoryPort inventoryRepositoryPort
    ) {
        return new InventoryDomainService(inventoryRepositoryPort);
    }

    @Bean
    public ManageProductUseCase manageProductUseCase(
            ProductRepositoryPort productRepositoryPort
    ) {
        return new ProductApplicationService(productRepositoryPort);
    }

    @Bean
    public ManageCartUseCase manageCartUseCase(
            ShoppingCartRepositoryPort shoppingCartRepositoryPort
    ) {
        return new CartDomainService(shoppingCartRepositoryPort);
    }

    @Bean
    public AuthApplicationService authApplicationService(
            UserRepositoryPort userRepositoryPort,
            PasswordHasherPort passwordHasherPort,
            JwtTokenPort jwtTokenPort
    ) {
        return new AuthApplicationService(
                userRepositoryPort,
                passwordHasherPort,
                jwtTokenPort
        );
    }

    @Bean
    public CheckoutUseCase checkoutApplicationService(
            CreateOrderUseCase createOrderUseCase,
            ReserveInventoryUseCase reserveInventoryUseCase,
            VariantRepositoryPort variantRepositoryPort,
            OrderRepositoryPort orderRepositoryPort,
            InventoryRepositoryPort inventoryRepositoryPort,
            ShoppingCartRepositoryPort shoppingCartRepositoryPort
    ) {
        return new CheckoutApplicationService(
                createOrderUseCase,
                reserveInventoryUseCase,
                variantRepositoryPort,
                orderRepositoryPort,
                inventoryRepositoryPort,
                shoppingCartRepositoryPort,
                DatabaseSeeder.DEFAULT_WAREHOUSE_ID
        );
    }

    @Bean
    public GetSalesReportUseCase getSalesReportUseCase(
            OrderRepositoryPort orderRepositoryPort
    ) {
        return new ReportApplicationService(orderRepositoryPort);
    }
}
