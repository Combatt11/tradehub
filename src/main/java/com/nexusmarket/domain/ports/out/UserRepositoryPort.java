package com.nexusmarket.domain.ports.out;

import com.nexusmarket.domain.models.User;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida para la persistencia de usuarios y sus credenciales.
 */
public interface UserRepositoryPort {

    User save(User user, String passwordHash);

    Optional<User> findById(UUID id);

    Optional<User> findByEmail(String email);

    Optional<UserCredentials> findCredentialsByEmail(String email);

    boolean existsByEmail(String email);

    /**
     * Usuario junto con su hash de contrasena, usado solo durante el login.
     */
    record UserCredentials(User user, String passwordHash) {

        public UserCredentials {
            Objects.requireNonNull(user, "user must not be null");
            Objects.requireNonNull(passwordHash, "passwordHash must not be null");
        }
    }
}
