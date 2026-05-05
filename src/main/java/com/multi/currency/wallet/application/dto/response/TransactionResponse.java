package com.multi.currency.wallet.application.dto.response;

import java.math.BigDecimal;

import com.multi.currency.wallet.domain.model.Transaction;
import java.time.Instant;
import java.util.UUID;
import com.multi.currency.wallet.domain.model.TransactionStatus;
import com.multi.currency.wallet.domain.model.TransactionType;

public record TransactionResponse(
        UUID id,
        UUID accountId,
        TransactionType type,
        BigDecimal amount,
        String currencyCode,
        BigDecimal balanceAfter,
        String description,
        UUID referenceTransactionId,
        UUID relatedAccountId,
        TransactionStatus status,
        Instant createdAt) {
    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getAccountId(),
                transaction.getType(),
                transaction.getAmount().getAmount(),
                transaction.getAmount().getCurrencyCode(),
                transaction.getBalanceAfter().getAmount(),
                transaction.getDescription(),
                transaction.getReferenceTransactionId(),
                transaction.getRelatedAccountId(),
                transaction.getStatus(),
                transaction.getCreatedAt());
    }
}
