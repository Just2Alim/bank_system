package kz.sim.bank.simulation.domain;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/** Calculates business time without changing security or retry wall-clock semantics. */
public final class VirtualBusinessClock {

    private final Clock wallClock;

    public VirtualBusinessClock(Clock wallClock) {
        this.wallClock = Objects.requireNonNull(wallClock, "wall clock must not be null");
    }

    public Instant now(SimulationState state) {
        Objects.requireNonNull(state, "simulation state must not be null");
        if (state.status() != SimulationStatus.RUNNING) {
            return state.simulationTime();
        }

        Duration wallElapsed = Duration.between(state.wallAnchor(), wallClock.instant());
        if (wallElapsed.isNegative()) {
            return state.simulationTime();
        }
        return state.simulationTime().plus(wallElapsed.multipliedBy(state.speed().multiplier()));
    }
}
