package kz.sim.bank.primitives.identifier;

import java.util.Objects;
import java.util.UUID;

final class IdentifierRules {

    static final UUID NIL_UUID = new UUID(0L, 0L);

    private IdentifierRules() {
    }

    static UUID requireUuidV7(UUID value, String label) {
        Objects.requireNonNull(value, label + " must not be null");
        if (NIL_UUID.equals(value)) {
            throw new IllegalArgumentException(label + " must not be the nil UUID");
        }
        if (value.version() != 7 || value.variant() != 2) {
            throw new IllegalArgumentException(label + " must be an RFC 9562 UUIDv7");
        }
        return value;
    }

    static UUID parseUuidV7(String value, String label) {
        Objects.requireNonNull(value, label + " must not be null");
        return requireUuidV7(UUID.fromString(value), label);
    }
}
