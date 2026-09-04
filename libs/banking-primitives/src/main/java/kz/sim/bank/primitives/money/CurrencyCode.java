package kz.sim.bank.primitives.money;

import java.util.Locale;
import java.util.Objects;

/** A canonical three-letter currency code used by the simulator ledger. */
public record CurrencyCode(String value) {

    public static final CurrencyCode KZT = new CurrencyCode("KZT");
    public static final CurrencyCode USD = new CurrencyCode("USD");
    public static final CurrencyCode EUR = new CurrencyCode("EUR");

    public CurrencyCode {
        Objects.requireNonNull(value, "currency code must not be null");
        if (!value.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException("Currency code must contain exactly three uppercase letters");
        }
    }

    public static CurrencyCode of(String value) {
        Objects.requireNonNull(value, "currency code must not be null");
        return new CurrencyCode(value.strip().toUpperCase(Locale.ROOT));
    }

    /** KZT is the sole movement currency in the current simulator scope. */
    public boolean isMovementEnabled() {
        return equals(KZT);
    }

    @Override
    public String toString() {
        return value;
    }
}
