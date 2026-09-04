package kz.sim.bank.platform.core.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CommandContextTest {

    @Test
    void trimsAndRetainsBoundedAuditAndIdempotencyValues() {
        var context = new CommandContext(" operator-7 ", " transfer-42 ");

        assertThat(context.actorId()).isEqualTo("operator-7");
        assertThat(context.idempotencyKey()).isEqualTo("transfer-42");
    }

    @Test
    void rejectsBlankOrOversizedBoundaryValues() {
        assertThatThrownBy(() -> new CommandContext(" ", "key"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CommandContext("actor", "x".repeat(129)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
