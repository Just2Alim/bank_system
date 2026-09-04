package kz.sim.bank.primitives.ledger;

import java.util.Objects;
import kz.sim.bank.primitives.identifier.AccountId;
import kz.sim.bank.primitives.money.Money;

/** One immutable debit or credit line in a journal. */
public record Posting(AccountId accountId, LedgerSide side, Money amount, String narrative) {

    public Posting {
        Objects.requireNonNull(accountId, "posting account id must not be null");
        Objects.requireNonNull(side, "posting side must not be null");
        Objects.requireNonNull(amount, "posting amount must not be null");
        Objects.requireNonNull(narrative, "posting narrative must not be null");
        narrative = narrative.strip();
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("Posting amount must be positive");
        }
        if (narrative.isEmpty()) {
            throw new IllegalArgumentException("Posting narrative must not be blank");
        }
    }

    public static Posting debit(AccountId accountId, Money amount, String narrative) {
        return new Posting(accountId, LedgerSide.DEBIT, amount, narrative);
    }

    public static Posting credit(AccountId accountId, Money amount, String narrative) {
        return new Posting(accountId, LedgerSide.CREDIT, amount, narrative);
    }
}
