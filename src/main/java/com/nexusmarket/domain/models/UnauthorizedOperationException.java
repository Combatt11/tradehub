package com.nexusmarket.domain.models;

/**
 * Se lanza cuando un usuario intenta operar sobre un recurso que no le
 * pertenece.
 */
public final class UnauthorizedOperationException extends RuntimeException {

    public UnauthorizedOperationException(String message) {
        super(message);
    }
}
