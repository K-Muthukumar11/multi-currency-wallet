package com.multi.currency.wallet.domain.model;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.multi.currency.wallet.domain.exception.InsufficientFundsException;
import com.multi.currency.wallet.domain.exception.InvalidAccountOperationException;

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

    /**
     * Credits (deposits) an amount to this account.
     * 
     * @param amount to be deposited.
     */
    public void credit(Money amount) {
        assertActive();
        assertSameCurrency(amount);
        if (!amount.isPositive()) {
            throw new InvalidAccountOperationException("Credit amount must be positive");
        }
        this.balance = this.balance.add(amount);
        this.updatedAt = Instant.now();
    }

    /**
     * Debits (withdraws) an amount from this account.
     * 
     * @param amount to be withdrawn.
     */
    public void debit(Money amount) {
        assertActive();
        assertSameCurrency(amount);
        if (!amount.isPositive()) {
            throw new InvalidAccountOperationException("Debit amount must be positive");
        }
        if (!this.balance.isGreaterThanOrEqual(amount)) {
            throw new InsufficientFundsException(
                    "Insufficient fuinds in account " + accountNumber + ". Available: " + balance + ", Requested: "
                            + amount);
        }
        this.balance = this.balance.subtract(amount);
        this.updatedAt = Instant.now();
    }

    private void assertActive() {
        if (this.status != AccountStatus.ACTIVE) {
            throw new InvalidAccountOperationException("Account " + accountNumber + " is not active");
        }
    }

    private void assertSameCurrency(Money amount) {
        if (!amount.getCurrencyCode().equals(this.currencyCode)) {
            throw new InvalidAccountOperationException(
                    "Currency mismatch. Account currency: " + currencyCode +
                            ", Operation  currency: " + amount.getCurrencyCode());

        }
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
