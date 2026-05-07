package com.multi.currency.wallet.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.Currency;

import com.multi.currency.wallet.domain.exception.InvalidMoneyException;

public class Money {

    public static final int SCALE = 3;
    public static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    private final BigDecimal amount;
    private final String currencyCode;

    private Money(BigDecimal amount, String currencyCode) {
        this.amount = amount.setScale(SCALE, ROUNDING_MODE);
        this.currencyCode = currencyCode;
    }

    public static Money of(BigDecimal amount, String currencyCode) {
        validateCurrency(currencyCode);
        if (amount == null) {
            throw new InvalidMoneyException("Amount cannot be null");
        }
        return new Money(amount, currencyCode);
    }

    public static Money of(String amount, String currencyCode) {
        try {
            return of(new BigDecimal(amount), currencyCode);
        } catch (NumberFormatException e) {
            throw new InvalidMoneyException("Invalid amount format: " + amount);
        }
    }

    public static Money zero(String currencyCode) {
        return of(BigDecimal.ZERO, currencyCode);
    }

    private static void validateCurrency(String currencyCode) {
        if (currencyCode == null || currencyCode.isBlank()) {
            throw new InvalidMoneyException("Currency code cannot be blank");
        }
        try {
            Currency.getInstance(currencyCode);
        } catch (IllegalArgumentException e) {
            throw new InvalidMoneyException("Invalid ISO 4217 currency code: " + currencyCode);
        }
    }

    public Money add(Money other) {
        assertSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currencyCode);
    }

    public Money subtract(Money other) {
        assertSameCurrency(other);
        return new Money(this.amount.subtract(other.amount), this.currencyCode);
    }

    public boolean isPositive() {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isNegative() {
        return amount.compareTo(BigDecimal.ZERO) < 0;
    }

    public boolean isZero() {
        return amount.compareTo(BigDecimal.ZERO) == 0;
    }

    public boolean isGreaterThanOrEqual(Money other) {
        assertSameCurrency(other);
        return this.amount.compareTo(other.amount) >= 0;
    }

    public boolean isSameCurrency(Money other) {
        return this.currencyCode.equals(other.currencyCode);
    }

    private void assertSameCurrency(Money other) {
        if (!isSameCurrency(other)) {
            throw new InvalidMoneyException(
                    "Cannot operate on different currencies: " + this.currencyCode + " vs " + other.currencyCode);
        }
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Money money))
            return false;
        return amount.compareTo(money.amount) == 0 && Objects.equals(currencyCode, money.currencyCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount.stripTrailingZeros(), currencyCode);
    }

    @Override
    public String toString() {
        return amount.toPlainString() + " " + currencyCode;
    }

}
