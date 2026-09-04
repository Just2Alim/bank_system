package kz.sim.bank.simulation.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class VirtualBusinessClockTest {

    private static final Instant WALL_BASE = Instant.parse("2026-09-04T10:00:00Z");
    private static final Instant BUSINESS_BASE = Instant.parse("2026-09-01T00:00:00Z");

    @Test
    void advancesRunningBusinessTimeByConfiguredMultiplier() {
        SimulationState running = SimulationState.initial(
                        SimulationProfile.DEMO, 42L, BUSINESS_BASE, WALL_BASE)
                .start(0L, WALL_BASE)
                .markRunning(1L, WALL_BASE)
                .changeSpeed(2L, SimulationSpeed.X60, BUSINESS_BASE, WALL_BASE);
        Clock wallClock = Clock.fixed(WALL_BASE.plusSeconds(10), ZoneOffset.UTC);

        assertThat(new VirtualBusinessClock(wallClock).now(running))
                .isEqualTo(BUSINESS_BASE.plusSeconds(600));
    }

    @Test
    void pausedAndStoppedBusinessTimeDoNotAdvance() {
        Clock wallClock = Clock.fixed(WALL_BASE.plusSeconds(500), ZoneOffset.UTC);
        SimulationState stopped = SimulationState.initial(
                SimulationProfile.SMALL, 7L, BUSINESS_BASE, WALL_BASE);
        SimulationState paused = stopped.start(0L, WALL_BASE)
                .markRunning(1L, WALL_BASE)
                .pause(2L, BUSINESS_BASE.plusSeconds(20), WALL_BASE.plusSeconds(1));

        VirtualBusinessClock clock = new VirtualBusinessClock(wallClock);
        assertThat(clock.now(stopped)).isEqualTo(BUSINESS_BASE);
        assertThat(clock.now(paused)).isEqualTo(BUSINESS_BASE.plusSeconds(20));
    }
}
