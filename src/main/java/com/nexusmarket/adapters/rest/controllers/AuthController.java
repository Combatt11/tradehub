package com.nexusmarket.adapters.rest.controllers;

import com.nexusmarket.adapters.rest.dto.AuthResponseDTO;
import com.nexusmarket.adapters.rest.dto.LoginRequestDTO;
import com.nexusmarket.adapters.rest.dto.RegisterRequestDTO;
import com.nexusmarket.adapters.rest.mappers.AuthRestMapper;
import com.nexusmarket.application.services.AuthApplicationService;
import com.nexusmarket.application.services.AuthApplicationService.AuthResult;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * API publica de registro e inicio de sesion.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthApplicationService authApplicationService;

    public AuthController(AuthApplicationService authApplicationService) {
        this.authApplicationService = Objects.requireNonNull(
                authApplicationService, "authApplicationService must not be null");
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(
            @Valid @RequestBody RegisterRequestDTO request
    ) {
        AuthResult result = authApplicationService.register(
                request.email(),
                request.password(),
                request.role(),
                AuthRestMapper.toDocumentId(request)
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AuthRestMapper.toResponse(result));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(
            @Valid @RequestBody LoginRequestDTO request
    ) {
        AuthResult result = authApplicationService.login(
                request.email(),
                request.password()
        );

        return ResponseEntity.ok(AuthRestMapper.toResponse(result));
    }
}
