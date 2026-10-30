package com.nexusmarket.adapters.rest.controllers;

import com.nexusmarket.adapters.security.AuthenticatedUser;
import com.nexusmarket.adapters.security.JwtService;
import com.nexusmarket.adapters.security.SecurityConfiguration;
import com.nexusmarket.domain.models.Money;
import com.nexusmarket.domain.models.Product;
import com.nexusmarket.domain.models.ProductStatus;
import com.nexusmarket.domain.models.ProductType;
import com.nexusmarket.domain.models.UserRole;
import com.nexusmarket.domain.models.Variant;
import com.nexusmarket.domain.ports.in.ManageProductUseCase;
import com.nexusmarket.domain.ports.out.UserRepositoryPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@Import(SecurityConfiguration.class)
class ProductControllerTest {

    private static final String PRODUCT_JSON = """
            {
              "name": "Test Product",
              "description": "Description",
              "type": "PHYSICAL",
              "variants": [
                {
                  "sku": "SKU-001",
                  "price": 10.00,
                  "currency": "USD"
                }
              ]
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ManageProductUseCase manageProductUseCase;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepositoryPort userRepositoryPort;

    @Test
    void postWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PRODUCT_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void postWithBuyerTokenReturnsForbidden() throws Exception {
        Authentication buyerAuthentication = authenticationFor(
                UserRole.BUYER,
                UUID.randomUUID()
        );

        mockMvc.perform(post("/api/products")
                        .with(authentication(buyerAuthentication))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PRODUCT_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void postWithSellerTokenReturnsCreated() throws Exception {
        UUID sellerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();

        Product product = new Product(
                productId,
                sellerId,
                "Test Product",
                "Description",
                ProductType.PHYSICAL,
                ProductStatus.DRAFT,
                List.of(new Variant(
                        variantId,
                        "SKU-001",
                        Money.of("10.00", "USD")
                ))
        );

        when(manageProductUseCase.createProduct(
                eq(sellerId),
                eq("Test Product"),
                eq("Description"),
                eq(ProductType.PHYSICAL),
                anyList()
        )).thenReturn(product);

        mockMvc.perform(post("/api/products")
                        .with(authentication(authenticationFor(UserRole.SELLER, sellerId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PRODUCT_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(productId.toString()))
                .andExpect(jsonPath("$.sellerId").value(sellerId.toString()))
                .andExpect(jsonPath("$.name").value("Test Product"));
    }

    private Authentication authenticationFor(UserRole role, UUID userId) {
        AuthenticatedUser principal = new AuthenticatedUser(
                userId,
                "user@nexusmarket.test",
                role
        );

        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
        );
    }
}
