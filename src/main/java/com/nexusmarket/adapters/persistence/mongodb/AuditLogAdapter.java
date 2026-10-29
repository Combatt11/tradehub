package com.nexusmarket.adapters.persistence.mongodb;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Adaptador de escritura de eventos de auditoria en MongoDB.
 */
@Component
public class AuditLogAdapter {

    private final MongoAuditRepository auditRepository;

    public AuditLogAdapter(MongoAuditRepository auditRepository) {
        this.auditRepository = Objects.requireNonNull(
                auditRepository, "auditRepository must not be null");
    }

    public AuditLogDocument log(String eventType, String entityId, String payload) {
        Objects.requireNonNull(eventType, "eventType must not be null");
        Objects.requireNonNull(entityId, "entityId must not be null");

        AuditLogDocument document = new AuditLogDocument(
                UUID.randomUUID().toString(),
                eventType,
                entityId,
                Instant.now(),
                payload
        );

        return auditRepository.save(document);
    }

    public AuditLogDocument log(String eventType, UUID entityId, String payload) {
        Objects.requireNonNull(entityId, "entityId must not be null");
        return log(eventType, entityId.toString(), payload);
    }
}
