package com.multi.currency.wallet.domain.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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
}
