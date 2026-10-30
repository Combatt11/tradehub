package com.nexusmarket.infrastructure.seed;

import com.nexusmarket.adapters.persistence.jpa.JpaWarehouseRepository;
import com.nexusmarket.adapters.persistence.jpa.WarehouseEntity;
import com.nexusmarket.domain.models.DocumentId;
import com.nexusmarket.domain.models.IdentityDocumentType;
import com.nexusmarket.domain.models.User;
import com.nexusmarket.domain.models.UserRole;
import com.nexusmarket.domain.ports.out.PasswordHasherPort;
import com.nexusmarket.domain.ports.out.UserRepositoryPort;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;

/**
 * Inicializa los datos minimos necesarios para operar.
 */
@Component
public class DatabaseSeeder implements CommandLineRunner {

    public static final UUID DEFAULT_WAREHOUSE_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

    private static final String DEFAULT_WAREHOUSE_NAME = "Default Warehouse";
    private static final String DEFAULT_ADMIN_EMAIL = "admin@nexusmarket.com";
    private static final String DEFAULT_ADMIN_PASSWORD = "admin123";
    private static final DocumentId DEFAULT_ADMIN_DOCUMENT =
            new DocumentId(IdentityDocumentType.DNI, "00000000", "ES");

    private final JpaWarehouseRepository warehouseRepository;
    private final UserRepositoryPort userRepositoryPort;
    private final PasswordHasherPort passwordHasherPort;

    public DatabaseSeeder(
            JpaWarehouseRepository warehouseRepository,
            UserRepositoryPort userRepositoryPort,
            PasswordHasherPort passwordHasherPort
    ) {
        this.warehouseRepository = Objects.requireNonNull(
                warehouseRepository, "warehouseRepository must not be null");
        this.userRepositoryPort = Objects.requireNonNull(
                userRepositoryPort, "userRepositoryPort must not be null");
        this.passwordHasherPort = Objects.requireNonNull(
                passwordHasherPort, "passwordHasherPort must not be null");
    }

    @Override
    public void run(String... args) {
        seedWarehouse();
        seedAdminUser();
    }

    private void seedWarehouse() {
        if (!warehouseRepository.existsById(DEFAULT_WAREHOUSE_ID)) {
            warehouseRepository.save(new WarehouseEntity(
                    DEFAULT_WAREHOUSE_ID,
                    DEFAULT_WAREHOUSE_NAME
            ));
        }
    }

    private void seedAdminUser() {
        if (userRepositoryPort.existsByEmail(DEFAULT_ADMIN_EMAIL)) {
            return;
        }

        User admin = User.create(
                UUID.randomUUID(),
                DEFAULT_ADMIN_DOCUMENT,
                DEFAULT_ADMIN_EMAIL,
                UserRole.ADMINISTRATOR
        );

        userRepositoryPort.save(admin, passwordHasherPort.encode(DEFAULT_ADMIN_PASSWORD));
    }
}
