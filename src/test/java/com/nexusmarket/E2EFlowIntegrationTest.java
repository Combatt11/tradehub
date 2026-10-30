package com.nexusmarket;

import com.nexusmarket.adapters.persistence.mongodb.MongoAuditRepository;
import com.nexusmarket.adapters.rest.dto.AddCartItemRequestDTO;
import com.nexusmarket.adapters.rest.dto.AuthResponseDTO;
import com.nexusmarket.adapters.rest.dto.CartResponseDTO;
import com.nexusmarket.adapters.rest.dto.CheckoutResponseDTO;
import com.nexusmarket.adapters.rest.dto.InventoryRequestDTO;
import com.nexusmarket.adapters.rest.dto.InventoryResponseDTO;
import com.nexusmarket.adapters.rest.dto.LoginRequestDTO;
import com.nexusmarket.adapters.rest.dto.PaymentResponseDTO;
import com.nexusmarket.adapters.rest.dto.ProductRequestDTO;
import com.nexusmarket.adapters.rest.dto.ProductResponseDTO;
import com.nexusmarket.adapters.rest.dto.RegisterRequestDTO;
import com.nexusmarket.adapters.rest.dto.VariantRequestDTO;
import com.nexusmarket.domain.models.IdentityDocumentType;
import com.nexusmarket.domain.models.Money;
import com.nexusmarket.domain.models.Order;
import com.nexusmarket.domain.models.OrderStatus;
import com.nexusmarket.domain.models.ProductType;
import com.nexusmarket.domain.models.UserRole;
import com.nexusmarket.domain.ports.out.OrderRepositoryPort;
import com.nexusmarket.infrastructure.seed.DatabaseSeeder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class E2EFlowIntegrationTest {

    private static final String ADMIN_EMAIL = "admin@nexusmarket.com";
    private static final String ADMIN_PASSWORD = "admin123";
    private static final String PASSWORD = "Secret123!";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private OrderRepositoryPort orderRepository;

    @MockitoBean
    private MongoAuditRepository mongoAuditRepository;

    @Test
    void completePurchaseFlowEndToEnd() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String sellerEmail = "seller-" + suffix + "@nexusmarket.test";
        String buyerEmail = "buyer-" + suffix + "@nexusmarket.test";

        register(
                sellerEmail,
                UserRole.SELLER,
                IdentityDocumentType.DNI,
                "12345678",
                "ES"
        );
        String sellerToken = login(sellerEmail, PASSWORD);

        ProductResponseDTO product = createProduct(sellerToken, suffix);
        assertFalse(product.variants().isEmpty());
        UUID variantId = product.variants().get(0).id();

        String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        addInventory(adminToken, variantId);

        register(buyerEmail, UserRole.BUYER, null, null, null);
        String buyerToken = login(buyerEmail, PASSWORD);

        addToCart(buyerToken, variantId);
        UUID orderId = checkout(buyerToken);

        PaymentResponseDTO payment = pay(buyerToken, orderId);
        assertTrue(payment.successful());
        assertNotNull(payment.transactionId());

        Order persistedOrder = orderRepository.findById(orderId).orElseThrow();
        assertEquals(OrderStatus.PAID, persistedOrder.getStatus());
        assertEquals(Money.of("50.00", "USD"), persistedOrder.getTotalAmount());
    }

    private void register(
            String email,
            UserRole role,
            IdentityDocumentType documentType,
            String documentNumber,
            String documentCountry
    ) {
        RegisterRequestDTO request = new RegisterRequestDTO(
                email,
                PASSWORD,
                role,
                documentType,
                documentNumber,
                documentCountry
        );

        ResponseEntity<AuthResponseDTO> response = restTemplate.exchange(
                "/api/auth/register",
                HttpMethod.POST,
                jsonEntity(request),
                AuthResponseDTO.class
        );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(role, response.getBody().role());
    }

    private String login(String email, String password) {
        LoginRequestDTO request = new LoginRequestDTO(email, password);

        ResponseEntity<AuthResponseDTO> response = restTemplate.exchange(
                "/api/auth/login",
                HttpMethod.POST,
                jsonEntity(request),
                AuthResponseDTO.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().token());
        return response.getBody().token();
    }

    private ProductResponseDTO createProduct(String sellerToken, String suffix) {
        ProductRequestDTO request = new ProductRequestDTO(
                "E2E Product " + suffix,
                "Product created by the end-to-end integration test",
                ProductType.PHYSICAL,
                List.of(new VariantRequestDTO(
                        null,
                        "SKU-" + suffix,
                        new BigDecimal("25.00"),
                        "USD",
                        Map.of()
                ))
        );

        ResponseEntity<ProductResponseDTO> response = restTemplate.exchange(
                "/api/products",
                HttpMethod.POST,
                jsonEntity(request, sellerToken),
                ProductResponseDTO.class
        );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().variants().size());
        return response.getBody();
    }

    private void addInventory(String adminToken, UUID variantId) {
        InventoryRequestDTO request = new InventoryRequestDTO(
                variantId,
                DatabaseSeeder.DEFAULT_WAREHOUSE_ID,
                10
        );

        ResponseEntity<InventoryResponseDTO> response = restTemplate.exchange(
                "/api/inventory/add",
                HttpMethod.POST,
                jsonEntity(request, adminToken),
                InventoryResponseDTO.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(10, response.getBody().availableStock());
        assertEquals(0, response.getBody().reservedStock());
    }

    private void addToCart(String buyerToken, UUID variantId) {
        AddCartItemRequestDTO request = new AddCartItemRequestDTO(variantId, 2);

        ResponseEntity<CartResponseDTO> response = restTemplate.exchange(
                "/api/cart/items",
                HttpMethod.POST,
                jsonEntity(request, buyerToken),
                CartResponseDTO.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().items().size());
        assertEquals(variantId, response.getBody().items().get(0).variantId());
        assertEquals(2, response.getBody().items().get(0).quantity());
    }

    private UUID checkout(String buyerToken) {
        ResponseEntity<CheckoutResponseDTO> response = restTemplate.exchange(
                "/api/checkout",
                HttpMethod.POST,
                new HttpEntity<>(bearerHeaders(buyerToken)),
                CheckoutResponseDTO.class
        );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().orderIds().size());
        return response.getBody().orderIds().get(0);
    }

    private PaymentResponseDTO pay(String buyerToken, UUID orderId) {
        ResponseEntity<PaymentResponseDTO> response = restTemplate.exchange(
                "/api/orders/" + orderId + "/pay",
                HttpMethod.POST,
                new HttpEntity<>(bearerHeaders(buyerToken)),
                PaymentResponseDTO.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        return response.getBody();
    }

    private HttpEntity<Object> jsonEntity(Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private HttpEntity<Object> jsonEntity(Object body, String token) {
        HttpHeaders headers = bearerHeaders(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private HttpHeaders bearerHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }
}
