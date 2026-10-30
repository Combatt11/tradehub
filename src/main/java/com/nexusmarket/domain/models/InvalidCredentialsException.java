package com.nexusmarket.domain.models;

/**
 * Excepcion de dominio para credenciales de autenticacion invalidas.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid email or password");
    }

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
