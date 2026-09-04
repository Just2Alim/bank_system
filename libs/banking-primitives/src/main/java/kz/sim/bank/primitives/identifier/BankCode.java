package kz.sim.bank.primitives.identifier;

import java.util.Locale;
import java.util.Objects;

/** Synthetic participant code embedded in public business identifiers. */
public record BankCode(String value) {

    public static final BankCode ORDA = new BankCode("ORDA");
    public static final BankCode NOMAD = new BankCode("NOMAD");
    public static final BankCode TENGRI = new BankCode("TENGRI");
    public static final BankCode NPP = new BankCode("NPP");
    public static final BankCode SYSTEM = new BankCode("SYSTEM");

    public BankCode {
        Objects.requireNonNull(value, "bank code must not be null");
        if (!value.matches("[A-Z0-9]{2,12}")) {
            throw new IllegalArgumentException("Bank code must contain 2-12 uppercase letters or digits");
        }
    }

    public static BankCode of(String value) {
        Objects.requireNonNull(value, "bank code must not be null");
        return new BankCode(value.strip().toUpperCase(Locale.ROOT));
    }

    @Override
    public String toString() {
        return value;
    }
}
