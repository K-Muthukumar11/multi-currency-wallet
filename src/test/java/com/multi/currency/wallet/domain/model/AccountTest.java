package com.multi.currency.wallet.domain.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.multi.currency.wallet.domain.exception.InsufficientFundsException;
import com.multi.currency.wallet.domain.exception.InvalidAccountOperationException;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Account")
class AccountTest {

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    // ── Factory ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("open")
    class Open {

        @Test
        void createsActiveAccountWithZeroBalance() {
            Account account = Account.open(userId, "USD");

            assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
            assertThat(account.getBalance().isZero()).isTrue();
            assertThat(account.getCurrencyCode()).isEqualTo("USD");
            assertThat(account.getUserId()).isEqualTo(userId);
        }

        @Test
        void generatesUniqueIds() {
            Account a1 = Account.open(userId, "USD");
            Account a2 = Account.open(userId, "USD");
            assertThat(a1.getId()).isNotEqualTo(a2.getId());
        }

        @Test
        void generates12DigitAccountNumber() {
            Account account = Account.open(userId, "USD");
            assertThat(account.getAccountNumber()).hasSize(12).matches("\\d{12}");
        }

        @Test
        void setsCreatedAtAndUpdatedAt() {
            Instant before = Instant.now();
            Account account = Account.open(userId, "USD");
            Instant after = Instant.now();

            assertThat(account.getCreatedAt()).isBetween(before, after);
            assertThat(account.getUpdatedAt()).isBetween(before, after);
        }
    }

    // ── credit ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("credit")
    class Credit {

        @Test
        void increasesBalanceByDepositAmount() {
            Account account = Account.open(userId, "USD");
            account.credit(Money.of("200", "USD"));
            assertThat(account.getBalance().getAmount()).isEqualByComparingTo("200");
        }

        @Test
        void accumulatesMultipleCredits() {
            Account account = Account.open(userId, "USD");
            account.credit(Money.of("100", "USD"));
            account.credit(Money.of("50.5", "USD"));
            assertThat(account.getBalance().getAmount()).isEqualByComparingTo("150.5");
        }

        @Test
        void throwsWhenAmountIsZero() {
            Account account = Account.open(userId, "USD");
            assertThatThrownBy(() -> account.credit(Money.zero("USD")))
                    .isInstanceOf(InvalidAccountOperationException.class)
                    .hasMessageContaining("Credit amount must be positive");
        }

        @Test
        void throwsWhenAmountIsNegative() {
            Account account = Account.open(userId, "USD");
            Money negAmount = Money.of("10", "USD").subtract(Money.of("20", "USD"));
            assertThatThrownBy(() -> account.credit(negAmount))
                    .isInstanceOf(InvalidAccountOperationException.class);
        }

        @Test
        void throwsOnCurrencyMismatch() {
            Account account = Account.open(userId, "USD");
            assertThatThrownBy(() -> account.credit(Money.of("100", "EUR")))
                    .isInstanceOf(InvalidAccountOperationException.class)
                    .hasMessageContaining("Currency mismatch");
        }

        @Test
        void updatesUpdatedAt() throws InterruptedException {
            Account account = Account.open(userId, "USD");
            Instant before = account.getUpdatedAt();
            Thread.sleep(10);
            account.credit(Money.of("1", "USD"));
            assertThat(account.getUpdatedAt()).isAfter(before);
        }
    }

    // ── debit ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("debit")
    class Debit {

        @Test
        void decreasesBalanceByWithdrawalAmount() {
            Account account = Account.open(userId, "USD");
            account.credit(Money.of("500", "USD"));
            account.debit(Money.of("200", "USD"));
            assertThat(account.getBalance().getAmount()).isEqualByComparingTo("300");
        }

        @Test
        void throwsWhenInsufficientFunds() {
            Account account = Account.open(userId, "USD");
            account.credit(Money.of("50", "USD"));
            assertThatThrownBy(() -> account.debit(Money.of("100", "USD")))
                    .isInstanceOf(InsufficientFundsException.class);
        }

        @Test
        void throwsWhenAmountIsZero() {
            Account account = Account.open(userId, "USD");
            account.credit(Money.of("100", "USD"));
            assertThatThrownBy(() -> account.debit(Money.zero("USD")))
                    .isInstanceOf(InvalidAccountOperationException.class)
                    .hasMessageContaining("Debit amount must be positive");
        }

        @Test
        void throwsOnCurrencyMismatch() {
            Account account = Account.open(userId, "USD");
            account.credit(Money.of("100", "USD"));
            assertThatThrownBy(() -> account.debit(Money.of("50", "EUR")))
                    .isInstanceOf(InvalidAccountOperationException.class)
                    .hasMessageContaining("Currency mismatch");
        }

        @Test
        void allowsDebitingExactBalance() {
            Account account = Account.open(userId, "USD");
            account.credit(Money.of("100", "USD"));
            account.debit(Money.of("100", "USD"));
            assertThat(account.getBalance().isZero()).isTrue();
        }
    }

}
