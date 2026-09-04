package kz.sim.bank.primitives.identifier;

import java.util.UUID;

public record CommandId(UUID value) implements TypedIdentifier {

    public CommandId {
        value = IdentifierRules.requireUuidV7(value, "command id");
    }

    public static CommandId of(UUID value) {
        return new CommandId(value);
    }

    public static CommandId parse(String value) {
        return new CommandId(IdentifierRules.parseUuidV7(value, "command id"));
    }

    public static CommandId random() {
        return new CommandId(Uuid7Generator.next());
    }

    @Override
    public String externalForm() {
        return value.toString();
    }
}
