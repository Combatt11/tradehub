package com.nexusmarket.adapters.rest.handlers;

import com.nexusmarket.adapters.rest.dto.ErrorResponseDTO;
import com.nexusmarket.domain.models.InsufficientStockException;
import com.nexusmarket.domain.models.InvalidCredentialsException;
import com.nexusmarket.domain.models.InvalidOrderStateException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * Traduccion centralizada de excepciones a respuestas HTTP.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponseDTO> handleNotFound(NoSuchElementException exception) {
        return buildError(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDTO> handleBadRequest(IllegalArgumentException exception) {
        return buildError(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponseDTO> handleInvalidCredentials(
            InvalidCredentialsException exception
    ) {
        return buildError(HttpStatus.UNAUTHORIZED, exception.getMessage());
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ErrorResponseDTO> handleInsufficientStock(
            InsufficientStockException exception
    ) {
        return buildError(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(InvalidOrderStateException.class)
    public ResponseEntity<ErrorResponseDTO> handleInvalidOrderState(
            InvalidOrderStateException exception
    ) {
        return buildError(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidation(
            MethodArgumentNotValidException exception
    ) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));

        if (message.isBlank()) {
            message = "Validation failed";
        }

        return buildError(HttpStatus.BAD_REQUEST, message);
    }

    private ResponseEntity<ErrorResponseDTO> buildError(HttpStatus status, String message) {
        ErrorResponseDTO body = new ErrorResponseDTO(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message
        );

        return ResponseEntity.status(status).body(body);
    }
}
