package com.nexusmarket.adapters.persistence.mongodb;

import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Repositorio Spring Data MongoDB para la auditoria.
 */
public interface MongoAuditRepository extends MongoRepository<AuditLogDocument, String> {
}
