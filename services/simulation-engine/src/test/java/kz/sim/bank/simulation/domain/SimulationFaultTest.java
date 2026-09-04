package kz.sim.bank.simulation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class SimulationFaultTest {

    private static final Instant NOW = Instant.parse("2026-09-04T10:00:00Z");

    @Test
    void createsAndClearsABoundedWallClockFaultImmutably() {
        SimulationFault active = SimulationFault.activate(
                "SIM-FLT-NPP-01K4A2X9V7A8Z5N3C1M6Q0R2TB",
                FaultType.BANK_UNAVAILABLE,
                "bank-platform-nomad",
                NOW,
                Duration.ofSeconds(30),
                "operator-001");

        SimulationFault cleared = active.clear(NOW.plusSeconds(5));

        assertThat(active.statusAt(NOW.plusSeconds(10))).isEqualTo(FaultStatus.ACTIVE);
        assertThat(cleared.statusAt(NOW.plusSeconds(10))).isEqualTo(FaultStatus.CLEARED);
        assertThat(active.clearedAt()).isEmpty();
        assertThat(cleared.revision()).isEqualTo(2L);
    }

    @Test
    void expiresByWallClockAndRejectsUnboundedTtl() {
        SimulationFault active = SimulationFault.activate(
                "SIM-FLT-NPP-01K4A2X9V7A8Z5N3C1M6Q0R2TB",
                FaultType.NPP_UNAVAILABLE,
                "national-payment-platform",
                NOW,
                Duration.ofSeconds(1),
                "operator-001");

        assertThat(active.statusAt(NOW.plusSeconds(1))).isEqualTo(FaultStatus.EXPIRED);
        assertThatThrownBy(() -> SimulationFault.activate(
                        "SIM-FLT-NPP-01K4A2X9V7A8Z5N3C1M6Q0R2TB",
                        FaultType.NPP_UNAVAILABLE,
                        "national-payment-platform",
                        NOW,
                        Duration.ofSeconds(901),
                        "operator-001"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsDoubleClear() {
        SimulationFault cleared = SimulationFault.activate(
                        "SIM-FLT-NPP-01K4A2X9V7A8Z5N3C1M6Q0R2TB",
                        FaultType.RTGS_QUEUE_DELAY,
                        "national-payment-platform",
                        NOW,
                        Duration.ofSeconds(60),
                        "operator-001")
                .clear(NOW.plusSeconds(1));

        assertThatThrownBy(() -> cleared.clear(NOW.plusSeconds(2)))
                .isInstanceOf(IllegalStateException.class);
    }
}
