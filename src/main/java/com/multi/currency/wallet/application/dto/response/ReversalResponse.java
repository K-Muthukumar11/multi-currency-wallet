package com.multi.currency.wallet.application.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.multi.currency.wallet.domain.model.TransactionType;

public record ReversalResponse(
        UUID reversalTransactionId,
        UUID originalTransactionId,
        TransactionType reversalType,
        BigDecimal amount,
        String currencyCode,
        BigDecimal balanceAfter,
        String description,
        Instant createdAt) {
}
