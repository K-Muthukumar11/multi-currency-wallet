package com.multi.currency.wallet.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.multi.currency.wallet.domain.model.TransactionStatus;
import com.multi.currency.wallet.domain.model.TransactionType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_transactions_account_id", columnList = "account_id"),
        @Index(name = "idx_transactions_created_at", columnList = "created_at"),
        @Index(name = "idx_transactions_reference_id", columnList = "reference_transaction_id")
})
@Getter
@Setter
@NoArgsConstructor
public class TransactionJpaEntity {
    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "account_id", nullable = false, columnDefinition = "uuid")
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private TransactionType type;

    @Column(name = "amount", nullable = false, precision = 20, scale = 3)
    private BigDecimal amount;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "balance_after", nullable = false, precision = 20, scale = 3)
    private BigDecimal balanceAfter;

    @Column(name = "balance_after_currency", nullable = false, length = 3)
    private String balanceAfterCurrency;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "reference_transaction_id", columnDefinition = "uuid")
    private UUID referenceTransactionId;

    @Column(name = "related_account_id", columnDefinition = "uuid")
    private UUID relatedAccountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TransactionStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

}
