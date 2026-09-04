package kz.sim.bank.primitives.hold;

import java.util.Objects;

public enum HoldStatus {
    ACTIVE,
    CAPTURED,
    RELEASED,
    EXPIRED;

    public boolean canTransitionTo(HoldStatus next) {
        Objects.requireNonNull(next, "next hold status must not be null");
        return this == ACTIVE && next != ACTIVE;
    }
}
