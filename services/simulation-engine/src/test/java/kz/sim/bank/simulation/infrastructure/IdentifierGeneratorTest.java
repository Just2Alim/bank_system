package kz.sim.bank.simulation.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IdentifierGeneratorTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-09-04T10:00:00Z"), ZoneOffset.UTC);

    @Test
    void createsCanonicalUuidV7() {
        UUID value = new UuidV7Generator(CLOCK).next();

        assertThat(value.version()).isEqualTo(7);
        assertThat(value.variant()).isEqualTo(2);
        assertThat(value.toString()).isLowerCase();
    }

    @Test
    void createsVisiblySyntheticFaultUlid() {
        String value = new UlidGenerator(CLOCK).get();

        assertThat(value).matches("^SIM-FLT-NPP-[0-9A-HJKMNP-TV-Z]{26}$");
    }
}
