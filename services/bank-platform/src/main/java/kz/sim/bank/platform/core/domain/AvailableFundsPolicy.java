package kz.sim.bank.platform.core.domain;

import java.util.Objects;
import kz.sim.bank.primitives.money.Money;

/** Deposit accounts are liabilities; available funds equal the book credit balance minus active holds. */
public final class AvailableFundsPolicy {

    public Money calculate(Money bookBalance, Money activeHolds) {
        Objects.requireNonNull(bookBalance, "book balance must not be null");
        Objects.requireNonNull(activeHolds, "active holds must not be null");
        if (bookBalance.isNegative() || activeHolds.isNegative()) {
            throw new IllegalArgumentException("book balance and active holds cannot be negative");
        }
        return bookBalance.subtract(activeHolds);
    }

    public void requireSufficient(Money bookBalance, Money activeHolds, Money requested) {
        Objects.requireNonNull(requested, "requested amount must not be null");
        if (!requested.isPositive()) {
            throw new IllegalArgumentException("requested amount must be positive");
        }
        Money available = calculate(bookBalance, activeHolds);
        if (available.compareTo(requested) < 0) {
            throw new InsufficientFundsException(
                    "Insufficient available funds: available=" + available.amount() + ", requested=" + requested.amount());
        }
    }
}
