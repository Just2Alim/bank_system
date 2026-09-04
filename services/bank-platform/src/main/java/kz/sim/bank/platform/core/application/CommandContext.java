package kz.sim.bank.platform.core.application;

import java.util.Objects;

/** Trusted caller identity plus the caller-supplied mutation de-duplication key. */
public record CommandContext(String actorId, String idempotencyKey) {

    public CommandContext {
        actorId = bounded(actorId, "actor id", 128);
        idempotencyKey = bounded(idempotencyKey, "idempotency key", 128);
    }

    private static String bounded(String value, String label, int maxLength) {
        Objects.requireNonNull(value, label + " must not be null");
        value = value.strip();
        if (value.isEmpty() || value.length() > maxLength) {
            throw new IllegalArgumentException(label + " must contain 1-" + maxLength + " characters");
        }
        return value;
    }
}
