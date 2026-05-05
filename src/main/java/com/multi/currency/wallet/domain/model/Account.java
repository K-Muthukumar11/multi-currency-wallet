package com.multi.currency.wallet.domain.model;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Account {

    private final UUID id;
    private final String accountNumber;
    private final UUID userId;
    private Money balance;
    private final String currencyCode;
    private AccountStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    private static final SecureRandom random = new SecureRandom();

    public Account(UUID id, String accountNumber, UUID userId, Money balance,
            String currencyCode, AccountStatus status, Instant createdAt, Instant updatedAt) {

        this.id = Objects.requireNonNull(id);
        this.accountNumber = Objects.requireNonNull(accountNumber);
        this.userId = Objects.requireNonNull(userId);
        this.balance = Objects.requireNonNull(balance);
        this.currencyCode = Objects.requireNonNull(currencyCode);
        this.status = Objects.requireNonNull(status);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);

    }

    public static Account open(UUID userId, String currencyCode) {
        Instant now = Instant.now();
        return new Account(UUID.randomUUID(), generateAccountNumber(), userId, Money.zero(currencyCode), currencyCode,
                AccountStatus.ACTIVE, now, now);
    }

    private static String generateAccountNumber() {
        // 12-digit numeci account number
        long number = random.nextLong(900_000_000_000L) + 100_000_000_000L;
        return String.valueOf(number);
    }

    // Getters (no setters - mutations only through domain methods)
    public UUID getId() {
        return id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public UUID getUserId() {
        return userId;
    }

    public Money getBalance() {
        return balance;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
