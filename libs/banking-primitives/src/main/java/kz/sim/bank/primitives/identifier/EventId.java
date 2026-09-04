package kz.sim.bank.primitives.identifier;

import java.util.UUID;

/** Technical event-envelope identifier encoded as RFC 9562 UUIDv7. */
public record EventId(UUID value) implements TypedIdentifier {

    public EventId {
        value = IdentifierRules.requireUuidV7(value, "event id");
    }

    public static EventId of(UUID value) {
        return new EventId(value);
    }

    public static EventId parse(String value) {
        return new EventId(IdentifierRules.parseUuidV7(value, "event id"));
    }

    public static EventId random() {
        return new EventId(Uuid7Generator.next());
    }

    @Override
    public String externalForm() {
        return value.toString();
    }
}
