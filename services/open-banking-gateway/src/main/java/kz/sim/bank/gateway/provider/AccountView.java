package kz.sim.bank.gateway.provider;

import java.math.BigDecimal;
import java.util.Objects;

public record AccountView(
        String id,
        String iban,
        String displayName,
        String type,
        String currency,
        BigDecimal bookBalance,
        BigDecimal availableBalance,
        String status,
        String provider) {
    public AccountView {
        Objects.requireNonNull(id);
        Objects.requireNonNull(iban);
        Objects.requireNonNull(displayName);
        Objects.requireNonNull(currency);
        bookBalance = bookBalance.setScale(2);
        availableBalance = availableBalance.setScale(2);
    }
}
