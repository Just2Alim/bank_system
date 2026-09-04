package kz.sim.bank.simulation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class SimulationStateTest {

    private static final Instant INITIAL_TIME = Instant.parse("2026-09-01T00:00:00Z");
    private static final Instant WALL_TIME = Instant.parse("2026-09-04T10:00:00Z");

    @Test
    void followsTheLegalLifecycleWithoutMutatingPreviousSnapshots() {
        SimulationState stopped = SimulationState.initial(SimulationProfile.DEMO, 42L, INITIAL_TIME, WALL_TIME);

        SimulationState starting = stopped.start(0L, WALL_TIME.plusSeconds(1));
        SimulationState running = starting.markRunning(1L, WALL_TIME.plusSeconds(2));
        SimulationState paused = running.pause(2L, INITIAL_TIME.plusSeconds(600), WALL_TIME.plusSeconds(3));
        SimulationState resumed = paused.resume(3L, WALL_TIME.plusSeconds(4));
        SimulationState stopping = resumed.stop(4L, INITIAL_TIME.plusSeconds(900), WALL_TIME.plusSeconds(5));
        SimulationState finished = stopping.markStopped(5L, WALL_TIME.plusSeconds(6));

        assertThat(stopped.status()).isEqualTo(SimulationStatus.STOPPED);
        assertThat(starting.status()).isEqualTo(SimulationStatus.STARTING);
        assertThat(running.status()).isEqualTo(SimulationStatus.RUNNING);
        assertThat(paused.status()).isEqualTo(SimulationStatus.PAUSED);
        assertThat(resumed.status()).isEqualTo(SimulationStatus.RUNNING);
        assertThat(stopping.status()).isEqualTo(SimulationStatus.STOPPING);
        assertThat(finished.status()).isEqualTo(SimulationStatus.STOPPED);
        assertThat(finished.revision()).isEqualTo(6L);
    }

    @Test
    void rejectsAStaleRevisionAndIllegalTransition() {
        SimulationState stopped = SimulationState.initial(SimulationProfile.SMALL, 7L, INITIAL_TIME, WALL_TIME);

        assertThatThrownBy(() -> stopped.start(9L, WALL_TIME.plusSeconds(1)))
                .isInstanceOf(StaleSimulationRevisionException.class);
        assertThatThrownBy(() -> stopped.pause(0L, INITIAL_TIME, WALL_TIME.plusSeconds(1)))
                .isInstanceOf(IllegalSimulationTransitionException.class);
    }

    @Test
    void changesSpeedOnlyForActiveSimulation() {
        SimulationState stopped = SimulationState.initial(SimulationProfile.DEMO, 42L, INITIAL_TIME, WALL_TIME);
        SimulationState running = stopped.start(0L, WALL_TIME.plusSeconds(1))
                .markRunning(1L, WALL_TIME.plusSeconds(2));

        SimulationState accelerated = running.changeSpeed(
                2L, SimulationSpeed.X60, INITIAL_TIME.plusSeconds(10), WALL_TIME.plusSeconds(3));

        assertThat(accelerated.speed()).isEqualTo(SimulationSpeed.X60);
        assertThat(accelerated.simulationTime()).isEqualTo(INITIAL_TIME.plusSeconds(10));
        assertThat(running.speed()).isEqualTo(SimulationSpeed.X1);
        assertThatThrownBy(() -> stopped.changeSpeed(0L, SimulationSpeed.X10, INITIAL_TIME, WALL_TIME))
                .isInstanceOf(IllegalSimulationTransitionException.class);
    }

    @Test
    void resetRequiresStoppedDemoStateAndExplicitPhrase() {
        SimulationState stopped = SimulationState.initial(SimulationProfile.DEMO, 42L, INITIAL_TIME, WALL_TIME);

        SimulationState reset = stopped.reset(
                0L, "RESET_DEMO", 99L, INITIAL_TIME.plusSeconds(5), WALL_TIME.plusSeconds(1));

        assertThat(reset.seed()).isEqualTo(99L);
        assertThat(reset.simulationTime()).isEqualTo(INITIAL_TIME.plusSeconds(5));
        assertThat(reset.revision()).isEqualTo(1L);
        assertThatThrownBy(() -> stopped.reset(0L, "wrong", 99L, INITIAL_TIME, WALL_TIME))
                .isInstanceOf(ResetNotAllowedException.class);

        SimulationState nonDemo = SimulationState.initial(SimulationProfile.LARGE, 42L, INITIAL_TIME, WALL_TIME);
        assertThatThrownBy(() -> nonDemo.reset(0L, "RESET_DEMO", 99L, INITIAL_TIME, WALL_TIME))
                .isInstanceOf(ResetNotAllowedException.class);
    }
}
