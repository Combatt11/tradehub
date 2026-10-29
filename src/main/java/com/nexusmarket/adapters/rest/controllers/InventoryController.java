package com.nexusmarket.adapters.rest.controllers;

import com.nexusmarket.adapters.rest.dto.InventoryRequestDTO;
import com.nexusmarket.adapters.rest.dto.InventoryResponseDTO;
import com.nexusmarket.adapters.rest.mappers.RestMapper;
import com.nexusmarket.domain.models.InventoryItem;
import com.nexusmarket.domain.ports.in.ReserveInventoryUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
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
public class InventoryController {

    private final ReserveInventoryUseCase reserveInventoryUseCase;

    public InventoryController(ReserveInventoryUseCase reserveInventoryUseCase) {
        this.reserveInventoryUseCase = Objects.requireNonNull(
                reserveInventoryUseCase, "reserveInventoryUseCase must not be null");
    }

    @PostMapping("/reserve")
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
}
