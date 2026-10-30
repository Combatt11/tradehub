package com.nexusmarket.adapters.security;

import com.nexusmarket.domain.ports.out.PasswordHasherPort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Adaptador de salida que implementa el cifrado de contrasenas con BCrypt.
 */
@Component
public class BCryptPasswordHasherAdapter implements PasswordHasherPort {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public String encode(String rawPassword) {
        return passwordEncoder.encode(Objects.requireNonNull(
                rawPassword, "rawPassword must not be null"));
    }

    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(
                Objects.requireNonNull(rawPassword, "rawPassword must not be null"),
                Objects.requireNonNull(encodedPassword, "encodedPassword must not be null")
        );
    }
}
