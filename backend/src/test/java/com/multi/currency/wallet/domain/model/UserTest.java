package com.multi.currency.wallet.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("User")
class UserTest {

    private final String email = "test@example.com";
    private final String passwordHash = "hashed_password_123";
    private final String fullName = "John Doe";

    // ── Factory / Registration ───────────────────────────────────────────────

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("should create user with default role and current timestamp")
        void createsUserWithDefaultValues() {
            Instant before = Instant.now();
            User user = User.register(email, passwordHash, fullName);
            Instant after = Instant.now();

            assertThat(user.getId()).isNotNull();
            assertThat(user.getEmail()).isEqualTo(email);
            assertThat(user.getPasswordHash()).isEqualTo(passwordHash);
            assertThat(user.getFullName()).isEqualTo(fullName);
            assertThat(user.getRole()).isEqualTo(UserRole.USER);
            assertThat(user.getCreatedAt()).isBetween(before, after);
        }

        @Test
        @DisplayName("should generate unique UUIDs for every registration")
        void generatesUniqueIds() {
            User user1 = User.register(email, passwordHash, fullName);
            User user2 = User.register(email, passwordHash, fullName);

            assertThat(user1.getId()).isNotEqualTo(user2.getId());
        }
    }

    // ── Validation ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("constructor validation")
    class ConstructorValidation {

        @Test
        @DisplayName("should throw NullPointerException if any required field is null")
        void throwsExceptionWhenFieldsAreNull() {
            UUID id = UUID.randomUUID();
            Instant now = Instant.now();

            assertThatThrownBy(() -> new User(null, email, passwordHash, fullName, UserRole.USER, now))
                    .isInstanceOf(NullPointerException.class);

            assertThatThrownBy(() -> new User(id, null, passwordHash, fullName, UserRole.USER, now))
                    .isInstanceOf(NullPointerException.class);

            assertThatThrownBy(() -> new User(id, email, null, fullName, UserRole.USER, now))
                    .isInstanceOf(NullPointerException.class);

            assertThatThrownBy(() -> new User(id, email, passwordHash, null, UserRole.USER, now))
                    .isInstanceOf(NullPointerException.class);

            assertThatThrownBy(() -> new User(id, email, passwordHash, fullName, null, now))
                    .isInstanceOf(NullPointerException.class);

            assertThatThrownBy(() -> new User(id, email, passwordHash, fullName, UserRole.USER, null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    // ── Domain Logic ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("properties")
    class Properties {

        @Test
        @DisplayName("should correctly return all assigned values")
        void returnsAssignedValues() {
            UUID id = UUID.randomUUID();
            Instant createdAt = Instant.parse("2023-01-01T10:00:00Z");
            
            User user = new User(id, email, passwordHash, fullName, UserRole.ADMIN, createdAt);

            assertThat(user.getId()).isEqualTo(id);
            assertThat(user.getEmail()).isEqualTo(email);
            assertThat(user.getPasswordHash()).isEqualTo(passwordHash);
            assertThat(user.getFullName()).isEqualTo(fullName);
            assertThat(user.getRole()).isEqualTo(UserRole.ADMIN);
            assertThat(user.getCreatedAt()).isEqualTo(createdAt);
        }
    }
}