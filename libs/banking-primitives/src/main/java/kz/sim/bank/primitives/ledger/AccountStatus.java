package kz.sim.bank.primitives.ledger;

import java.util.Objects;

public enum AccountStatus {
    PENDING_OPEN,
    ACTIVE,
    FROZEN,
    CLOSED;

    public boolean canTransitionTo(AccountStatus next) {
        Objects.requireNonNull(next, "next account status must not be null");
        return switch (this) {
            case PENDING_OPEN -> next == ACTIVE || next == CLOSED;
            case ACTIVE -> next == FROZEN || next == CLOSED;
            case FROZEN -> next == ACTIVE || next == CLOSED;
            case CLOSED -> false;
        };
    }

    void requireTransitionTo(AccountStatus next) {
        if (canTransitionTo(next)) {
            return;
        }
        if (this == CLOSED) {
            throw new IllegalStateException("Account state CLOSED is terminal");
        }
        throw new IllegalStateException("Illegal account state transition from " + this + " to " + next);
    }
}
