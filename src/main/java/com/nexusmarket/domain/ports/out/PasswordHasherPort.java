package com.nexusmarket.domain.ports.out;

/**
 * Puerto de salida para el cifrado y verificacion de contrasenas.
 */
public interface PasswordHasherPort {

    String encode(String rawPassword);

    boolean matches(String rawPassword, String encodedPassword);
}
