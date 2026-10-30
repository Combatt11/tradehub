package com.nexusmarket.adapters.rest.controllers;

import com.nexusmarket.adapters.rest.dto.InventoryRequestDTO;
import com.nexusmarket.adapters.rest.dto.InventoryResponseDTO;
import com.nexusmarket.adapters.rest.mappers.RestMapper;
import com.nexusmarket.domain.models.InventoryItem;
import com.nexusmarket.domain.ports.in.AddInventoryUseCase;
import com.nexusmarket.domain.ports.in.ReserveInventoryUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * API REST de inventario.
 */
@RestController
@RequestMapping("/api/inventory")
@Tag(name = "Inventario", description = "Operaciones de inventario")
@SecurityRequirement(name = "bearerAuth")
public class InventoryController {

    private final ReserveInventoryUseCase reserveInventoryUseCase;
    private final AddInventoryUseCase addInventoryUseCase;

    public InventoryController(
            ReserveInventoryUseCase reserveInventoryUseCase,
            AddInventoryUseCase addInventoryUseCase
    ) {
        this.reserveInventoryUseCase = Objects.requireNonNull(
                reserveInventoryUseCase, "reserveInventoryUseCase must not be null");
        this.addInventoryUseCase = Objects.requireNonNull(
                addInventoryUseCase, "addInventoryUseCase must not be null");
    }

    @PostMapping("/reserve")
    @PreAuthorize("hasAnyRole('SELLER', 'LOGISTICS_OPERATOR', 'ADMINISTRATOR')")
    @Operation(summary = "Reserva stock para una variante y almacén")
    public ResponseEntity<InventoryResponseDTO> reserveStock(
            @Valid @RequestBody InventoryRequestDTO request
    ) {
        InventoryItem item = reserveInventoryUseCase.reserveStock(
                request.variantId(),
                request.warehouseId(),
                request.quantity()
        );

        return ResponseEntity.ok(RestMapper.toInventoryResponse(item));
    }

    @PostMapping("/add")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMINISTRATOR')")
    @Operation(summary = "Añade stock disponible para una variante y almacén")
    public ResponseEntity<InventoryResponseDTO> addStock(
            @Valid @RequestBody InventoryRequestDTO request
    ) {
        InventoryItem item = addInventoryUseCase.addStock(
                request.variantId(),
                request.warehouseId(),
                request.quantity()
        );

        return ResponseEntity.ok(RestMapper.toInventoryResponse(item));
    }
}
