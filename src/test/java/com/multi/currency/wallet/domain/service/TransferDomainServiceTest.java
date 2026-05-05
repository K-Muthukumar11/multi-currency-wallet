package com.multi.currency.wallet.domain.service;

import com.multi.currency.wallet.domain.exception.InsufficientFundsException;
import com.multi.currency.wallet.domain.exception.InvalidAccountOperationException;
import com.multi.currency.wallet.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("TransferDomainService")
class TransferDomainServiceTest {

    private TransferDomainService service;
    private Account source;
    private Account destination;

    @BeforeEach
    void setUp() {
        service = new TransferDomainService();
        source = buildAccount(UUID.randomUUID(), "USD", "500");
        destination = buildAccount(UUID.randomUUID(), "USD", "100");
    }

    @Nested
    @DisplayName("transfer – happy path")
    class HappyPath {

        @Test
        void returnsExactlyTwoTransactions() {
            List<Transaction> txs = service.transfer(source, destination, Money.of("200", "USD"), "test");
            assertThat(txs).hasSize(2);
        }

        @Test
        void firstTransactionIsDebit() {
            List<Transaction> txs = service.transfer(source, destination, Money.of("200", "USD"), null);
            assertThat(txs.get(0).getType()).isEqualTo(TransactionType.TRANSFER_DEBIT);
        }

        @Test
        void secondTransactionIsCredit() {
            List<Transaction> txs = service.transfer(source, destination, Money.of("200", "USD"), null);
            assertThat(txs.get(1).getType()).isEqualTo(TransactionType.TRANSFER_CREDIT);
        }

        @Test
        void debitsSourceAccount() {
            service.transfer(source, destination, Money.of("200", "USD"), null);
            assertThat(source.getBalance().getAmount()).isEqualByComparingTo("300");
        }

        @Test
        void creditsDestinationAccount() {
            service.transfer(source, destination, Money.of("200", "USD"), null);
            assertThat(destination.getBalance().getAmount()).isEqualByComparingTo("300");
        }

        @Test
        void debitTxReferencesDestinationAccount() {
            List<Transaction> txs = service.transfer(source, destination, Money.of("100", "USD"), null);
            assertThat(txs.get(0).getRelatedAccountId()).isEqualTo(destination.getId());
        }

        @Test
        void creditTxReferencesSourceAccount() {
            List<Transaction> txs = service.transfer(source, destination, Money.of("100", "USD"), null);
            assertThat(txs.get(1).getRelatedAccountId()).isEqualTo(source.getId());
        }

        @Test
        void balanceAfterIsConsistentWithAccountBalance() {
            Money amount = Money.of("300", "USD");
            List<Transaction> txs = service.transfer(source, destination, amount, null);
            assertThat(txs.get(0).getBalanceAfter()).isEqualTo(source.getBalance());
            assertThat(txs.get(1).getBalanceAfter()).isEqualTo(destination.getBalance());
        }
    }

    @Nested
    @DisplayName("transfer – validation failures")
    class Validation {

        @Test
        void throwsWhenSourceAndDestinationAreTheSameAccount() {
            assertThatThrownBy(() -> service.transfer(source, source, Money.of("100", "USD"), null))
                    .isInstanceOf(InvalidAccountOperationException.class)
                    .hasMessageContaining("same account");
        }

        @Test
        void throwsWhenCurrenciesDiffer() {
            Account eurAccount = buildAccount(UUID.randomUUID(), "EUR", "200");
            assertThatThrownBy(() -> service.transfer(source, eurAccount, Money.of("100", "USD"), null))
                    .isInstanceOf(InvalidAccountOperationException.class)
                    .hasMessageContaining("Cross-currency");
        }

        @Test
        void throwsWhenAmountIsZero() {
            assertThatThrownBy(() -> service.transfer(source, destination, Money.zero("USD"), null))
                    .isInstanceOf(InvalidAccountOperationException.class)
                    .hasMessageContaining("amount must be positive");
        }

        @Test
        void throwsWhenAmountCurrencyDoesNotMatchAccountCurrency() {
            assertThatThrownBy(() -> service.transfer(source, destination, Money.of("100", "EUR"), null))
                    .isInstanceOf(InvalidAccountOperationException.class);
        }

        @Test
        void throwsWhenSourceHasInsufficientFunds() {
            assertThatThrownBy(() -> service.transfer(source, destination, Money.of("1000", "USD"), null))
                    .isInstanceOf(InsufficientFundsException.class);
        }
    }

    // ── helper ────────────────────────────────────────────────────────────────

    private Account buildAccount(UUID id, String currency, String balance) {
        return new Account(
                id,
                "1234" + id.toString().substring(0, 8),
                UUID.randomUUID(),
                Money.of(balance, currency),
                currency,
                AccountStatus.ACTIVE,
                Instant.now(),
                Instant.now());
    }
}
