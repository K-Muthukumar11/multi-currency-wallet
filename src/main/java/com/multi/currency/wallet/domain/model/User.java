package com.multi.currency.wallet.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class User {

    private final UUID id;
    private final String email;
    private final String passwordHash;
    private final String fullName;
    private final UserRole role;
    private final Instant createdAt;

    public User(UUID id, String email, String passwordHash, String fullName, UserRole role, Instant createdAt) {
        this.id = Objects.requireNonNull(id);
        this.email = Objects.requireNonNull(email);
        this.passwordHash = Objects.requireNonNull(passwordHash);
        this.fullName = Objects.requireNonNull(fullName);
        this.role = Objects.requireNonNull(role);
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public static User register(String email, String passwordHash, String fullName) {
        return new User(UUID.randomUUID(), email, passwordHash, fullName, UserRole.USER, Instant.now());
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public UserRole getRole() {
        return role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
