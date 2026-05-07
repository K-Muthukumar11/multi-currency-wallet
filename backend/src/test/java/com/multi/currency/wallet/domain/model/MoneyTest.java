package com.multi.currency.wallet.domain.model;

import com.multi.currency.wallet.domain.exception.InvalidMoneyException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Money")
class MoneyTest {

    // ── Factory methods ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("of(BigDecimal, String)")
    class OfBigDecimal {

        @Test
        void createsMoneyWithCorrectAmountAndCurrency() {
            Money money = Money.of(new BigDecimal("100.50"), "USD");
            assertThat(money.getAmount()).isEqualByComparingTo("100.500");
            assertThat(money.getCurrencyCode()).isEqualTo("USD");
        }

        @Test
        void scalesAmountToThreeDecimalPlaces() {
            Money money = Money.of(new BigDecimal("10.1"), "USD");
            assertThat(money.getAmount().scale()).isEqualTo(3);
        }

        @Test
        void roundsHalfUp() {
            Money money = Money.of(new BigDecimal("1.0005"), "USD");
            assertThat(money.getAmount()).isEqualByComparingTo("1.001");
        }

        @Test
        void throwsWhenAmountIsNull() {
            assertThatThrownBy(() -> Money.of((BigDecimal) null, "USD"))
                    .isInstanceOf(InvalidMoneyException.class)
                    .hasMessageContaining("Amount cannot be null");
        }

        @Test
        void throwsWhenCurrencyCodeIsNull() {
            assertThatThrownBy(() -> Money.of(BigDecimal.TEN, null))
                    .isInstanceOf(InvalidMoneyException.class);
        }

        @Test
        void throwsWhenCurrencyCodeIsBlank() {
            assertThatThrownBy(() -> Money.of(BigDecimal.TEN, "  "))
                    .isInstanceOf(InvalidMoneyException.class);
        }

        @Test
        void throwsForInvalidIso4217CurrencyCode() {
            assertThatThrownBy(() -> Money.of(BigDecimal.TEN, "XYZ"))
                    .isInstanceOf(InvalidMoneyException.class)
                    .hasMessageContaining("Invalid ISO 4217 currency code");
        }
    }

    @Nested
    @DisplayName("of(String, String)")
    class OfString {

        @Test
        void parsesValidStringAmount() {
            Money money = Money.of("250.75", "EUR");
            assertThat(money.getAmount()).isEqualByComparingTo("250.750");
        }

        @Test
        void throwsForInvalidNumberFormat() {
            assertThatThrownBy(() -> Money.of("not-a-number", "USD"))
                    .isInstanceOf(InvalidMoneyException.class)
                    .hasMessageContaining("Invalid amount format");
        }
    }

    @Nested
    @DisplayName("zero(String)")
    class Zero {

        @Test
        void createsZeroMoney() {
            Money zero = Money.zero("USD");
            assertThat(zero.isZero()).isTrue();
            assertThat(zero.getCurrencyCode()).isEqualTo("USD");
        }
    }

    // ── Arithmetic ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("add")
    class Add {

        @Test
        void addsTwoMoniesOfSameCurrency() {
            Money a = Money.of("100", "USD");
            Money b = Money.of("50.5", "USD");
            assertThat(a.add(b).getAmount()).isEqualByComparingTo("150.5");
        }

        @Test
        void throwsWhenCurrenciesDiffer() {
            Money usd = Money.of("100", "USD");
            Money eur = Money.of("100", "EUR");
            assertThatThrownBy(() -> usd.add(eur))
                    .isInstanceOf(InvalidMoneyException.class)
                    .hasMessageContaining("different currencies");
        }
    }

    @Nested
    @DisplayName("subtract")
    class Subtract {

        @Test
        void subtractsMoniesOfSameCurrency() {
            Money a = Money.of("100", "USD");
            Money b = Money.of("30", "USD");
            assertThat(a.subtract(b).getAmount()).isEqualByComparingTo("70");
        }

        @Test
        void allowsResultToBeNegative() {
            Money a = Money.of("10", "USD");
            Money b = Money.of("50", "USD");
            assertThat(a.subtract(b).isNegative()).isTrue();
        }

        @Test
        void throwsWhenCurrenciesDiffer() {
            Money usd = Money.of("100", "USD");
            Money gbp = Money.of("50", "GBP");
            assertThatThrownBy(() -> usd.subtract(gbp))
                    .isInstanceOf(InvalidMoneyException.class);
        }
    }

    // ── Comparison predicates ─────────────────────────────────────────────────

    @Nested
    @DisplayName("isPositive / isNegative / isZero")
    class Predicates {

        @Test
        void isPositiveReturnsTrueForPositiveAmount() {
            assertThat(Money.of("0.001", "USD").isPositive()).isTrue();
        }

        @Test
        void isPositiveReturnsFalseForZero() {
            assertThat(Money.zero("USD").isPositive()).isFalse();
        }

        @Test
        void isNegativeReturnsTrueForNegativeAmount() {
            Money neg = Money.of("10", "USD").subtract(Money.of("20", "USD"));
            assertThat(neg.isNegative()).isTrue();
        }

        @Test
        void isZeroReturnsTrueForZero() {
            assertThat(Money.zero("USD").isZero()).isTrue();
        }

        @Test
        void isZeroReturnsFalseForNonZero() {
            assertThat(Money.of("0.001", "USD").isZero()).isFalse();
        }
    }

    @Nested
    @DisplayName("isGreaterThanOrEqual")
    class IsGreaterThanOrEqual {

        @Test
        void returnsTrueWhenGreater() {
            assertThat(Money.of("100", "USD").isGreaterThanOrEqual(Money.of("50", "USD"))).isTrue();
        }

        @Test
        void returnsTrueWhenEqual() {
            assertThat(Money.of("50", "USD").isGreaterThanOrEqual(Money.of("50", "USD"))).isTrue();
        }

        @Test
        void returnsFalseWhenLess() {
            assertThat(Money.of("10", "USD").isGreaterThanOrEqual(Money.of("50", "USD"))).isFalse();
        }

        @Test
        void throwsWhenCurrenciesDiffer() {
            assertThatThrownBy(() -> Money.of("100", "USD").isGreaterThanOrEqual(Money.of("100", "EUR")))
                    .isInstanceOf(InvalidMoneyException.class);
        }
    }

    // ── equals / hashCode ────────────────────────────────────────────────────

    @Nested
    @DisplayName("equals and hashCode")
    class EqualsAndHashCode {

        @Test
        void equalWhenAmountAndCurrencyMatch() {
            Money a = Money.of("100.000", "USD");
            Money b = Money.of("100", "USD");
            assertThat(a).isEqualTo(b);
        }

        @Test
        void notEqualWhenAmountsDiffer() {
            assertThat(Money.of("100", "USD")).isNotEqualTo(Money.of("200", "USD"));
        }

        @Test
        void notEqualWhenCurrenciesDiffer() {
            assertThat(Money.of("100", "USD")).isNotEqualTo(Money.of("100", "EUR"));
        }

        @Test
        void sameHashCodeForEqualObjects() {
            assertThat(Money.of("100", "USD").hashCode())
                    .isEqualTo(Money.of("100.000", "USD").hashCode());
        }
    }

    // ── toString ─────────────────────────────────────────────────────────────

    @Test
    void toStringContainsAmountAndCurrency() {
        String str = Money.of("99.99", "USD").toString();
        assertThat(str).contains("99.990").contains("USD");
    }
}
