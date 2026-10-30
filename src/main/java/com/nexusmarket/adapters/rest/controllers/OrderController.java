package com.nexusmarket.adapters.rest.controllers;

import com.nexusmarket.adapters.rest.dto.OrderRequestDTO;
import com.nexusmarket.adapters.rest.dto.OrderResponseDTO;
import com.nexusmarket.adapters.rest.dto.PaymentResponseDTO;
import com.nexusmarket.adapters.rest.mappers.RestMapper;
import com.nexusmarket.adapters.security.AuthenticatedUser;
import com.nexusmarket.domain.models.Order;
import com.nexusmarket.domain.ports.in.CreateOrderUseCase;
import com.nexusmarket.domain.ports.in.ProcessPaymentUseCase;
import com.nexusmarket.domain.ports.out.PaymentGatewayPort.PaymentResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

/**
 * API REST de ordenes y pagos.
 */
@RestController
@RequestMapping("/api/orders")
@Tag(name = "Órdenes", description = "Operaciones de órdenes y pagos")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final CreateOrderUseCase createOrderUseCase;
    private final ProcessPaymentUseCase processPaymentUseCase;

    public OrderController(
            CreateOrderUseCase createOrderUseCase,
            ProcessPaymentUseCase processPaymentUseCase
    ) {
        this.createOrderUseCase = Objects.requireNonNull(
                createOrderUseCase, "createOrderUseCase must not be null");
        this.processPaymentUseCase = Objects.requireNonNull(
                processPaymentUseCase, "processPaymentUseCase must not be null");
    }

    @PostMapping
    @PreAuthorize("hasRole('BUYER')")
    @Operation(summary = "Crea una orden para un vendedor")
    public ResponseEntity<OrderResponseDTO> createOrder(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody OrderRequestDTO request
    ) {
        Order order = createOrderUseCase.createOrder(
                user.id(),
                request.sellerId(),
                RestMapper.toOrderLineInputs(request.lines())
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(RestMapper.toOrderResponse(order));
    }

    @PostMapping("/{id}/pay")
    @PreAuthorize("hasRole('BUYER')")
    @Operation(summary = "Procesa el pago de una orden del comprador autenticado")
    public ResponseEntity<PaymentResponseDTO> payOrder(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable("id") UUID id
    ) {
        PaymentResult result = processPaymentUseCase.processPayment(id, user.id());
        return ResponseEntity.ok(RestMapper.toPaymentResponse(result));
    }
}
