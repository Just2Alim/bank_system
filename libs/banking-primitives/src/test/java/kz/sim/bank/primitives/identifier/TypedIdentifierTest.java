package kz.sim.bank.primitives.identifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class TypedIdentifierTest {

    private static final UUID VALUE = UUID.fromString("018f47cc-6ad4-7a30-8000-000000000001");

    @Test
    void businessIdentifiersRoundTripOpaqueSyntheticForms() {
        String accountValue = "SIM-ACCOUNT-ORDA-01J6Z9D0M0ABCDEFGHJKMNPQRS";
        String journalValue = "SIM-JOURNAL-NOMAD-01J6Z9D0M0ABCDEFGHJKMNPQRS";
        String holdValue = "SIM-HOLD-TENGRI-01J6Z9D0M0ABCDEFGHJKMNPQRS";

        assertThat(AccountId.parse(accountValue).externalForm()).isEqualTo(accountValue);
        assertThat(JournalId.parse(journalValue).bankCode()).isEqualTo(BankCode.NOMAD);
        assertThat(HoldId.parse(holdValue).value()).isEqualTo(holdValue);
        assertThat(CommandId.of(VALUE).value()).isEqualTo(VALUE);
        assertThat(EventId.parse(VALUE.toString()).externalForm()).isEqualTo(VALUE.toString());
    }

    @Test
    void factoriesCreateScopedUlidsAndUuidV7TechnicalIds() {
        assertThat(AccountId.random(BankCode.ORDA).value())
                .matches("SIM-ACCOUNT-ORDA-[0-7][0-9A-HJKMNP-TV-Z]{25}");
        assertThat(JournalId.random(BankCode.NOMAD).value())
                .matches("SIM-JOURNAL-NOMAD-[0-7][0-9A-HJKMNP-TV-Z]{25}");
        assertThat(HoldId.random(BankCode.TENGRI).value())
                .matches("SIM-HOLD-TENGRI-[0-7][0-9A-HJKMNP-TV-Z]{25}");
        assertThat(CommandId.random().value().version()).isEqualTo(7);
        assertThat(CommandId.random().value().variant()).isEqualTo(2);
        assertThat(EventId.random().value().version()).isEqualTo(7);
    }

    @Test
    void identifiersRejectWrongTypeNilOrMalformedValues() {
        assertThatThrownBy(() -> AccountId.parse(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> AccountId.parse(
                "SIM-HOLD-ORDA-01J6Z9D0M0ABCDEFGHJKMNPQRS"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ACCOUNT");
        assertThatThrownBy(() -> HoldId.parse("not-a-uuid"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CommandId.of(new UUID(0L, 0L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nil UUID");
        assertThatThrownBy(() -> CommandId.of(UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("UUIDv7");
        assertThatThrownBy(() -> CommandId.parse(null)).isInstanceOf(NullPointerException.class);
    }
}
