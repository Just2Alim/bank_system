package kz.sim.bank.primitives.hold;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import kz.sim.bank.primitives.identifier.AccountId;
import kz.sim.bank.primitives.identifier.HoldId;
import kz.sim.bank.primitives.money.Money;

/** Immutable authorization hold; transitions produce replacement snapshots. */
public record AccountHold(
        HoldId id,
        AccountId accountId,
        Money amount,
        Instant createdAt,
        Instant expiresAt,
        HoldStatus status,
        Optional<Instant> resolvedAt) {

    public AccountHold {
        Objects.requireNonNull(id, "hold id must not be null");
        Objects.requireNonNull(accountId, "hold account id must not be null");
        Objects.requireNonNull(amount, "hold amount must not be null");
        Objects.requireNonNull(createdAt, "hold creation time must not be null");
        Objects.requireNonNull(expiresAt, "hold expiry time must not be null");
        Objects.requireNonNull(status, "hold status must not be null");
        Objects.requireNonNull(resolvedAt, "hold resolution must not be null");

        if (!id.bankCode().equals(accountId.bankCode())) {
            throw new IllegalArgumentException("Hold and account must belong to the same bank");
        }
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("Hold amount must be positive");
        }
        if (!expiresAt.isAfter(createdAt)) {
            throw new IllegalArgumentException("Hold expiry must be after creation");
        }
        if (status == HoldStatus.ACTIVE && resolvedAt.isPresent()) {
            throw new IllegalArgumentException("An active hold cannot have a resolution time");
        }
        if (status != HoldStatus.ACTIVE && resolvedAt.isEmpty()) {
            throw new IllegalArgumentException("A terminal hold requires a resolution time");
        }
        resolvedAt.ifPresent(resolution -> validateResolution(status, createdAt, expiresAt, resolution));
    }

    public static AccountHold create(
            HoldId id,
            AccountId accountId,
            Money amount,
            Instant createdAt,
            Instant expiresAt) {
        return new AccountHold(
                id, accountId, amount, createdAt, expiresAt, HoldStatus.ACTIVE, Optional.empty());
    }

    public AccountHold capture(Instant at) {
        return transitionTo(HoldStatus.CAPTURED, at);
    }

    public AccountHold release(Instant at) {
        return transitionTo(HoldStatus.RELEASED, at);
    }

    public AccountHold expire(Instant at) {
        return transitionTo(HoldStatus.EXPIRED, at);
    }

    private AccountHold transitionTo(HoldStatus next, Instant at) {
        Objects.requireNonNull(next, "next hold status must not be null");
        Objects.requireNonNull(at, "hold transition time must not be null");
        if (!status.canTransitionTo(next)) {
            if (status != HoldStatus.ACTIVE) {
                throw new IllegalStateException("Hold state " + status + " is terminal");
            }
            throw new IllegalStateException("Illegal hold state transition from " + status + " to " + next);
        }
        validateResolution(next, createdAt, expiresAt, at);
        return new AccountHold(id, accountId, amount, createdAt, expiresAt, next, Optional.of(at));
    }

    private static void validateResolution(
            HoldStatus status, Instant createdAt, Instant expiresAt, Instant resolution) {
        if (resolution.isBefore(createdAt)) {
            throw new IllegalArgumentException("Hold cannot resolve before creation");
        }
        if (status == HoldStatus.EXPIRED && resolution.isBefore(expiresAt)) {
            throw new IllegalArgumentException("Hold cannot expire before expiry time");
        }
        if (status != HoldStatus.EXPIRED && resolution.isAfter(expiresAt)) {
            throw new IllegalArgumentException("Hold cannot be captured or released after expiry");
        }
    }
}
