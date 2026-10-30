package com.nexusmarket.adapters.persistence.jpa;

import com.nexusmarket.domain.models.User;
import com.nexusmarket.domain.ports.out.UserRepositoryPort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador de salida que implementa {@link UserRepositoryPort} mediante JPA.
 */
@Repository
public class UserRepositoryAdapter implements UserRepositoryPort {

    private final JpaUserRepository userRepository;

    public UserRepositoryAdapter(JpaUserRepository userRepository) {
        this.userRepository = Objects.requireNonNull(
                userRepository, "userRepository must not be null");
    }

    @Override
    @Transactional
    public User save(User user, String passwordHash) {
        Objects.requireNonNull(user, "user must not be null");
        Objects.requireNonNull(passwordHash, "passwordHash must not be null");

        UserEntity entity = userRepository.findById(user.getId())
                .orElseGet(UserEntity::new);

        UserMapper.copyToEntity(user, passwordHash, entity);

        return UserMapper.toDomain(userRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        return userRepository.findById(id).map(UserMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(normalize(email))
                .map(UserMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserCredentials> findCredentialsByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(normalize(email))
                .map(entity -> new UserCredentials(
                        UserMapper.toDomain(entity),
                        entity.getPasswordHash()
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmailIgnoreCase(normalize(email));
    }

    private static String normalize(String email) {
        Objects.requireNonNull(email, "email must not be null");
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
