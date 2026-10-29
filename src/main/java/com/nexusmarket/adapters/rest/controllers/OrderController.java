package com.nexusmarket.adapters.rest.controllers;

import com.nexusmarket.adapters.rest.dto.OrderRequestDTO;
import com.nexusmarket.adapters.rest.dto.OrderResponseDTO;
import com.nexusmarket.adapters.rest.dto.PaymentResponseDTO;
import com.nexusmarket.adapters.rest.mappers.RestMapper;
import com.nexusmarket.domain.models.Order;
import com.nexusmarket.domain.ports.in.CreateOrderUseCase;
import com.nexusmarket.domain.ports.in.ProcessPaymentUseCase;
import com.nexusmarket.domain.ports.out.PaymentGatewayPort.PaymentResult;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<OrderResponseDTO> createOrder(
            @Valid @RequestBody OrderRequestDTO request
    ) {
        Order order = createOrderUseCase.createOrder(
                request.buyerId(),
                request.sellerId(),
                RestMapper.toOrderLineInputs(request.lines())
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(RestMapper.toOrderResponse(order));
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<PaymentResponseDTO> payOrder(
            @PathVariable("id") UUID id
    ) {
        PaymentResult result = processPaymentUseCase.processPayment(id);
        return ResponseEntity.ok(RestMapper.toPaymentResponse(result));
    }
}
