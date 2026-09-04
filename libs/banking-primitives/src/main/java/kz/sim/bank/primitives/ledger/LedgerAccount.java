package kz.sim.bank.primitives.ledger;

import java.util.Locale;
import java.util.Objects;
import kz.sim.bank.primitives.identifier.AccountId;
import kz.sim.bank.primitives.money.CurrencyCode;

/** Immutable chart-of-accounts metadata plus its lifecycle snapshot. */
public record LedgerAccount(
        AccountId id,
        String code,
        String name,
        LedgerAccountClass accountClass,
        CurrencyCode currency,
        AccountStatus status) {

    public LedgerAccount {
        Objects.requireNonNull(id, "account id must not be null");
        Objects.requireNonNull(code, "account code must not be null");
        Objects.requireNonNull(name, "account name must not be null");
        Objects.requireNonNull(accountClass, "account class must not be null");
        Objects.requireNonNull(currency, "account currency must not be null");
        Objects.requireNonNull(status, "account status must not be null");

        code = code.strip().toUpperCase(Locale.ROOT);
        name = name.strip();
        if (!code.matches("[A-Z0-9][A-Z0-9._-]{1,63}")) {
            throw new IllegalArgumentException("Account code must be 2-64 canonical characters");
        }
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Account name must not be blank");
        }
    }

    public LedgerAccount transitionTo(AccountStatus next) {
        status.requireTransitionTo(next);
        return new LedgerAccount(id, code, name, accountClass, currency, next);
    }
}
