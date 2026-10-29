package com.nexusmarket.infrastructure.seed;

import com.nexusmarket.adapters.persistence.jpa.JpaWarehouseRepository;
import com.nexusmarket.adapters.persistence.jpa.WarehouseEntity;
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

    private final JpaWarehouseRepository warehouseRepository;

    public DatabaseSeeder(JpaWarehouseRepository warehouseRepository) {
        this.warehouseRepository = Objects.requireNonNull(
                warehouseRepository, "warehouseRepository must not be null");
    }

    @Override
    public void run(String... args) {
        if (!warehouseRepository.existsById(DEFAULT_WAREHOUSE_ID)) {
            warehouseRepository.save(new WarehouseEntity(
                    DEFAULT_WAREHOUSE_ID,
                    DEFAULT_WAREHOUSE_NAME
            ));
        }
    }
}
