package com.multi.currency.wallet.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Transaction {
    private final UUID id;
    private final UUID accountId;
    private final TransactionType type;
    private final Money amount;
    private final Money balanceAfter;
    private final String description;
    private final UUID referenceTransactionId; // for reversals
    private final UUID relatedAccountId; // counterpart in a transfer
    private final TransactionStatus status;
    private final Instant createdAt;

    private Transaction(Builder builder) {
        this.id = Objects.requireNonNull(builder.id);
        this.accountId = Objects.requireNonNull(builder.accountId);
        this.type = Objects.requireNonNull(builder.type);
        this.amount = Objects.requireNonNull(builder.amount);
        this.balanceAfter = Objects.requireNonNull(builder.balanceAfter);
        this.description = builder.description;
        this.referenceTransactionId = builder.referenceTransactionId;
        this.relatedAccountId = builder.relatedAccountId;
        this.status = Objects.requireNonNull(builder.status);
        this.createdAt = Objects.requireNonNull(builder.createdAt);
    }

    public static Transaction createDeposit(UUID accountId, Money amount,
            Money balanceAfter, String description) {
        return new Builder()
                .id(UUID.randomUUID())
                .accountId(accountId)
                .type(TransactionType.DEPOSIT)
                .amount(amount)
                .balanceAfter(balanceAfter)
                .description(description != null ? description : "Deposit")
                .status(TransactionStatus.COMPLETED)
                .createdAt(Instant.now())
                .build();
    }

    public static Transaction createTransferDebit(UUID accountId, UUID relaatedAccountId, Money amount,
            Money balanceAfter, String description) {
        return new Builder()
                .id(UUID.randomUUID())
                .accountId(accountId)
                .relatedAccountId(relaatedAccountId)
                .type(TransactionType.TRANSFER_DEBIT)
                .amount(amount)
                .balanceAfter(balanceAfter)
                .description(description != null ? description : "Transfer out")
                .status(TransactionStatus.COMPLETED)
                .createdAt(Instant.now())
                .build();
    }

    public static Transaction createTransferCredit(UUID accountId, UUID relaatedAccountId, Money amount,
            Money balanceAfter, String description) {
        return new Builder()
                .id(UUID.randomUUID())
                .accountId(accountId)
                .relatedAccountId(relaatedAccountId)
                .type(TransactionType.TRANSFER_CREDIT)
                .amount(amount)
                .balanceAfter(balanceAfter)
                .description(description != null ? description : "Transfer in")
                .status(TransactionStatus.COMPLETED)
                .createdAt(Instant.now())
                .build();
    }

    public static Transaction createReversal(UUID accountId, UUID originalTransactionId, Money amount,
            Money balanceAfter, TransactionType originalType) {
        TransactionType reversalType = originalType.isDebit() ? TransactionType.REVERSAL_CREDIT
                : TransactionType.REVERSAL_DEBIT;
        return new Builder()
                .id(UUID.randomUUID())
                .accountId(accountId)
                .referenceTransactionId(originalTransactionId)
                .type(reversalType)
                .amount(amount)
                .balanceAfter(balanceAfter)
                .description("Reversal of transaction " + originalTransactionId)
                .status(TransactionStatus.REVERSED)
                .createdAt(Instant.now())
                .build();
    }

    // Getters only - immutable
    public UUID getId() {
        return id;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public TransactionType getType() {
        return type;
    }

    public Money getAmount() {
        return amount;
    }

    public Money getBalanceAfter() {
        return balanceAfter;
    }

    public String getDescription() {
        return description;
    }

    public UUID getReferenceTransactionId() {
        return referenceTransactionId;
    }

    public UUID getRelatedAccountId() {
        return relatedAccountId;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public static class Builder {
        private UUID id;
        private UUID accountId;
        private TransactionType type;
        private Money amount;
        private Money balanceAfter;
        private String description;
        private UUID referenceTransactionId;
        private UUID relatedAccountId;
        private TransactionStatus status;
        private Instant createdAt;

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder accountId(UUID accountId) {
            this.accountId = accountId;
            return this;
        }

        public Builder type(TransactionType type) {
            this.type = type;
            return this;
        }

        public Builder amount(Money amount) {
            this.amount = amount;
            return this;
        }

        public Builder balanceAfter(Money balanceAfter) {
            this.balanceAfter = balanceAfter;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder referenceTransactionId(UUID referenceTransactionId) {
            this.referenceTransactionId = referenceTransactionId;
            return this;
        }

        public Builder relatedAccountId(UUID relatedAccountId) {
            this.relatedAccountId = relatedAccountId;
            return this;
        }

        public Builder status(TransactionStatus status) {
            this.status = status;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Transaction build() {
            return new Transaction(this);
        }
    }
}
