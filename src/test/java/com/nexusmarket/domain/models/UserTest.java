package com.nexusmarket.domain.models;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserTest {

    private final UUID userId = UUID.randomUUID();
    private final DocumentId documentId = new DocumentId(
            IdentityDocumentType.DNI,
            "12345678",
            "ES"
    );

    @Test
    void createBuyerWithoutDocumentIsActiveAndNormalizesEmail() {
        User user = User.create(
                userId,
                null,
                "  Buyer@Example.COM ",
                UserRole.BUYER
        );

        assertEquals("buyer@example.com", user.getEmail());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertTrue(user.isActive());
    }

    @Test
    void createSellerWithDocumentIsActive() {
        User user = User.create(userId, documentId, "seller@example.com", UserRole.SELLER);

        assertEquals(documentId, user.getDocumentId());
        assertEquals(UserRole.SELLER, user.getRole());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
    }

    @Test
    void createSellerWithoutDocumentThrows() {
        assertThrows(
                IllegalArgumentException.class,
                () -> User.create(userId, null, "seller@example.com", UserRole.SELLER)
        );
    }

    @Test
    void createRejectsInvalidEmail() {
        assertThrows(
                IllegalArgumentException.class,
                () -> User.create(userId, null, "not-an-email", UserRole.BUYER)
        );
    }

    @Test
    void activateReactivatesBlockedUser() {
        User user = new User(
                userId,
                null,
                "buyer@example.com",
                UserRole.BUYER,
                UserStatus.BLOCKED
        );

        user.activate();

        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertTrue(user.isActive());
    }

    @Test
    void blockChangesActiveUserToBlocked() {
        User user = User.create(userId, null, "buyer@example.com", UserRole.BUYER);

        user.block();

        assertEquals(UserStatus.BLOCKED, user.getStatus());
    }

    @Test
    void blockThrowsWhenUserIsInactive() {
        User user = new User(
                userId,
                null,
                "buyer@example.com",
                UserRole.BUYER,
                UserStatus.INACTIVE
        );

        assertThrows(IllegalStateException.class, user::block);
        assertEquals(UserStatus.INACTIVE, user.getStatus());
    }
}
