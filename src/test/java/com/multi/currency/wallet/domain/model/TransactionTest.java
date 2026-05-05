package com.multi.currency.wallet.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Transaction")
class TransactionTest {

    private final UUID accountId = UUID.randomUUID();
    private final UUID relatedAccountId = UUID.randomUUID();
    private final Money amount = Money.of("100", "USD");
    private final Money balanceAfter = Money.of("900", "USD");

    @Nested
    @DisplayName("createDeposit")
    class CreateDeposit {

        @Test
        void createsDepositWithCorrectType() {
            Transaction tx = Transaction.createDeposit(accountId, amount, balanceAfter, "Test deposit");
            assertThat(tx.getType()).isEqualTo(TransactionType.DEPOSIT);
        }

        @Test
        void setsCompletedStatus() {
            Transaction tx = Transaction.createDeposit(accountId, amount, balanceAfter, null);
            assertThat(tx.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        }

        @Test
        void usesDefaultDescriptionWhenNullProvided() {
            Transaction tx = Transaction.createDeposit(accountId, amount, balanceAfter, null);
            assertThat(tx.getDescription()).isEqualTo("Deposit");
        }

        @Test
        void usesProvidedDescription() {
            Transaction tx = Transaction.createDeposit(accountId, amount, balanceAfter, "Salary");
            assertThat(tx.getDescription()).isEqualTo("Salary");
        }

        @Test
        void setsAmountAndBalanceAfter() {
            Transaction tx = Transaction.createDeposit(accountId, amount, balanceAfter, null);
            assertThat(tx.getAmount()).isEqualTo(amount);
            assertThat(tx.getBalanceAfter()).isEqualTo(balanceAfter);
        }

        @Test
        void hasNoRelatedAccountOrReference() {
            Transaction tx = Transaction.createDeposit(accountId, amount, balanceAfter, null);
            assertThat(tx.getRelatedAccountId()).isNull();
            assertThat(tx.getReferenceTransactionId()).isNull();
        }

        @Test
        void generatesUniqueIds() {
            Transaction t1 = Transaction.createDeposit(accountId, amount, balanceAfter, null);
            Transaction t2 = Transaction.createDeposit(accountId, amount, balanceAfter, null);
            assertThat(t1.getId()).isNotEqualTo(t2.getId());
        }
    }

    @Nested
    @DisplayName("createTransferDebit")
    class CreateTransferDebit {

        @Test
        void createsTransferDebitWithCorrectType() {
            Transaction tx = Transaction.createTransferDebit(accountId, relatedAccountId, amount, balanceAfter,
                    "Payment");
            assertThat(tx.getType()).isEqualTo(TransactionType.TRANSFER_DEBIT);
        }

        @Test
        void setsRelatedAccountId() {
            Transaction tx = Transaction.createTransferDebit(accountId, relatedAccountId, amount, balanceAfter, null);
            assertThat(tx.getRelatedAccountId()).isEqualTo(relatedAccountId);
        }

        @Test
        void usesDefaultDescriptionWhenNull() {
            Transaction tx = Transaction.createTransferDebit(accountId, relatedAccountId, amount, balanceAfter, null);
            assertThat(tx.getDescription()).isEqualTo("Transfer out");
        }

        @Test
        void setsCompletedStatus() {
            Transaction tx = Transaction.createTransferDebit(accountId, relatedAccountId, amount, balanceAfter, null);
            assertThat(tx.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        }
    }

    @Nested
    @DisplayName("createTransferCredit")
    class CreateTransferCredit {

        @Test
        void createsTransferCreditWithCorrectType() {
            Transaction tx = Transaction.createTransferCredit(accountId, relatedAccountId, amount, balanceAfter, null);
            assertThat(tx.getType()).isEqualTo(TransactionType.TRANSFER_CREDIT);
        }

        @Test
        void usesDefaultDescriptionWhenNull() {
            Transaction tx = Transaction.createTransferCredit(accountId, relatedAccountId, amount, balanceAfter, null);
            assertThat(tx.getDescription()).isEqualTo("Transfer in");
        }

        @Test
        void setsRelatedAccountId() {
            Transaction tx = Transaction.createTransferCredit(accountId, relatedAccountId, amount, balanceAfter, null);
            assertThat(tx.getRelatedAccountId()).isEqualTo(relatedAccountId);
        }
    }

    @Nested
    @DisplayName("createReversal")
    class CreateReversal {

        @Test
        void depositReversalCreatesReversalDebitType() {
            Transaction tx = Transaction.createReversal(accountId, UUID.randomUUID(), amount, balanceAfter,
                    TransactionType.DEPOSIT);
            assertThat(tx.getType()).isEqualTo(TransactionType.REVERSAL_DEBIT);
        }

        @Test
        void transferDebitReversalCreatesReversalCreditType() {
            // TRANSFER_DEBIT.isDebit() == true → reversed to REVERSAL_CREDIT
            Transaction tx = Transaction.createReversal(accountId, UUID.randomUUID(), amount, balanceAfter,
                    TransactionType.TRANSFER_DEBIT);
            assertThat(tx.getType()).isEqualTo(TransactionType.REVERSAL_CREDIT);
        }

        @Test
        void transferCreditReversalCreatesReversalDebitType() {
            // TRANSFER_CREDIT.isDebit() == false → reversed to REVERSAL_DEBIT
            Transaction tx = Transaction.createReversal(accountId, UUID.randomUUID(), amount, balanceAfter,
                    TransactionType.TRANSFER_CREDIT);
            assertThat(tx.getType()).isEqualTo(TransactionType.REVERSAL_DEBIT);
        }

        @Test
        void setsReversedStatus() {
            Transaction tx = Transaction.createReversal(accountId, UUID.randomUUID(), amount, balanceAfter,
                    TransactionType.DEPOSIT);
            assertThat(tx.getStatus()).isEqualTo(TransactionStatus.REVERSED);
        }

        @Test
        void setsReferenceTransactionId() {
            UUID originalId = UUID.randomUUID();
            Transaction tx = Transaction.createReversal(accountId, originalId, amount, balanceAfter,
                    TransactionType.DEPOSIT);
            assertThat(tx.getReferenceTransactionId()).isEqualTo(originalId);
        }

        @Test
        void descriptionContainsOriginalTransactionId() {
            UUID originalId = UUID.randomUUID();
            Transaction tx = Transaction.createReversal(accountId, originalId, amount, balanceAfter,
                    TransactionType.DEPOSIT);
            assertThat(tx.getDescription()).contains(originalId.toString());
        }
    }
}
