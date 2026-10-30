package com.nexusmarket.adapters.rest.controllers;

import com.nexusmarket.adapters.security.AuthenticatedUser;
import com.nexusmarket.adapters.security.JwtService;
import com.nexusmarket.adapters.security.SecurityConfiguration;
import com.nexusmarket.domain.models.Money;
import com.nexusmarket.domain.models.UserRole;
import com.nexusmarket.domain.ports.in.GetSalesReportUseCase;
import com.nexusmarket.domain.ports.in.SalesReportDTO;
import com.nexusmarket.domain.ports.out.UserRepositoryPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportController.class)
@Import(SecurityConfiguration.class)
class ReportControllerTest {

    private static final String START = "2026-10-01T00:00:00";
    private static final String END = "2026-10-31T23:59:59";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetSalesReportUseCase getSalesReportUseCase;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepositoryPort userRepositoryPort;

    @Test
    void getWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/reports/sales")
                        .param("start", START)
                        .param("end", END))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getWithBuyerTokenReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/reports/sales")
                        .with(authentication(authenticationFor(UserRole.BUYER, UUID.randomUUID())))
                        .param("start", START)
                        .param("end", END))
                .andExpect(status().isForbidden());
    }

    @Test
    void getWithAdminTokenReturnsSalesReport() throws Exception {
        LocalDateTime start = LocalDateTime.of(2026, 10, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 10, 31, 23, 59, 59);

        when(getSalesReportUseCase.getSalesReport(start, end))
                .thenReturn(new SalesReportDTO(
                        Money.of("150.00", "USD"),
                        2,
                        Money.of("75.00", "USD")
                ));

        mockMvc.perform(get("/api/reports/sales")
                        .with(authentication(authenticationFor(
                                UserRole.ADMINISTRATOR,
                                UUID.randomUUID())))
                        .param("start", START)
                        .param("end", END))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroOrdenes").value(2))
                .andExpect(jsonPath("$.totalFacturado.currency").value("USD"))
                .andExpect(jsonPath("$.totalFacturado.amount").isNumber())
                .andExpect(jsonPath("$.ticketPromedio.currency").value("USD"))
                .andExpect(jsonPath("$.ticketPromedio.amount").isNumber());
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
