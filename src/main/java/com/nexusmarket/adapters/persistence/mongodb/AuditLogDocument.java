package com.nexusmarket.adapters.persistence.mongodb;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Documento de auditoria almacenado en MongoDB.
 */
@Document(collection = "audit_logs")
public class AuditLogDocument {

    @Id
    private String id;

    private String eventType;

    private String entityId;

    private Instant timestamp;

    /**
     * Carga util serializada (normalmente JSON).
     */
    private String payload;

    public AuditLogDocument() {
    }

    public AuditLogDocument(
            String id,
            String eventType,
            String entityId,
            Instant timestamp,
            String payload
    ) {
        this.id = id;
        this.eventType = eventType;
        this.entityId = entityId;
        this.timestamp = timestamp;
        this.payload = payload;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }
}
