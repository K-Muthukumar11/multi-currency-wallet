package com.multi.currency.wallet.domain.model;

import java.time.Instant;

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
}
