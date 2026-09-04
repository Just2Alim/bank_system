package kz.sim.bank.primitives.money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * An immutable ledger amount represented with PostgreSQL {@code NUMERIC(19,2)} semantics.
 * Signed values are supported for arithmetic; financial postings impose positivity separately.
 */
public record Money(BigDecimal amount, CurrencyCode currency) implements Comparable<Money> {

    public static final int PRECISION = 19;
    public static final int SCALE = 2;

    public Money {
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        try {
            amount = amount.setScale(SCALE, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Money supports at most two fractional digits", exception);
        }
        if (amount.precision() > PRECISION) {
            throw new IllegalArgumentException("Money exceeds NUMERIC(19,2) precision");
        }
    }

    public static Money of(String amount, CurrencyCode currency) {
        Objects.requireNonNull(amount, "amount must not be null");
        return new Money(new BigDecimal(amount), currency);
    }

    public static Money ofMinor(long minorUnits, CurrencyCode currency) {
        return new Money(BigDecimal.valueOf(minorUnits, SCALE), currency);
    }

    public static Money zero(CurrencyCode currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public Money add(Money other) {
        requireSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    public Money subtract(Money other) {
        requireSameCurrency(other);
        return new Money(amount.subtract(other.amount), currency);
    }

    public Money negate() {
        return new Money(amount.negate(), currency);
    }

    public Money abs() {
        return isNegative() ? negate() : this;
    }

    public boolean isPositive() {
        return amount.signum() > 0;
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    @Override
    public int compareTo(Money other) {
        requireSameCurrency(other);
        return amount.compareTo(other.amount);
    }

    private void requireSameCurrency(Money other) {
        Objects.requireNonNull(other, "other money must not be null");
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    "Cannot combine different currency values: " + currency + " and " + other.currency);
        }
    }
}
