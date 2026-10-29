package com.nexusmarket.domain.models;

/**
 * Roles de usuario definidos en la especificacion. Cada cuenta tiene
 * exactamente un rol en esta version.
 */
public enum UserRole {
    BUYER,
    SELLER,
    LOGISTICS_OPERATOR,
    SUPERVISOR,
    ADMINISTRATOR;

    /**
     * RN-GEN-18: MFA obligatorio para roles privilegiados.
     */
    public boolean requiresMfa() {
        return this == LOGISTICS_OPERATOR || this == SUPERVISOR || this == ADMINISTRATOR;
    }

    /**
     * RN-USR-02: el documento es obligatorio salvo para compradores en
     * los supuestos generales.
     */
    public boolean requiresIdentityDocument() {
        return this != BUYER;
    }
}
