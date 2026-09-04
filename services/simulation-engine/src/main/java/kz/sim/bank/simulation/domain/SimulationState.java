package kz.sim.bank.simulation.domain;

import java.time.Instant;
import java.util.Objects;

/** Immutable snapshot of deterministic business-time control state. */
public record SimulationState(
        SimulationStatus status,
        SimulationProfile profile,
        long seed,
        SimulationSpeed speed,
        Instant simulationTime,
        Instant wallAnchor,
        long revision) {

    public SimulationState {
        Objects.requireNonNull(status, "simulation status must not be null");
        Objects.requireNonNull(profile, "simulation profile must not be null");
        Objects.requireNonNull(speed, "simulation speed must not be null");
        Objects.requireNonNull(simulationTime, "simulation time must not be null");
        Objects.requireNonNull(wallAnchor, "wall anchor must not be null");
        if (revision < 0) {
            throw new IllegalArgumentException("simulation revision must not be negative");
        }
    }

    public static SimulationState initial(
            SimulationProfile profile, long seed, Instant simulationTime, Instant wallTime) {
        return new SimulationState(
                SimulationStatus.STOPPED,
                profile,
                seed,
                SimulationSpeed.X1,
                simulationTime,
                wallTime,
                0L);
    }

    public SimulationState start(long expectedRevision, Instant wallTime) {
        requireRevision(expectedRevision);
        requireStatus(SimulationStatus.STOPPED, "start");
        return next(SimulationStatus.STARTING, speed, simulationTime, wallTime);
    }

    public SimulationState markRunning(long expectedRevision, Instant wallTime) {
        requireRevision(expectedRevision);
        requireStatus(SimulationStatus.STARTING, "mark as running");
        return next(SimulationStatus.RUNNING, speed, simulationTime, wallTime);
    }

    public SimulationState pause(
            long expectedRevision, Instant currentSimulationTime, Instant wallTime) {
        requireRevision(expectedRevision);
        requireStatus(SimulationStatus.RUNNING, "pause");
        return next(SimulationStatus.PAUSED, speed, currentSimulationTime, wallTime);
    }

    public SimulationState resume(long expectedRevision, Instant wallTime) {
        requireRevision(expectedRevision);
        requireStatus(SimulationStatus.PAUSED, "resume");
        return next(SimulationStatus.RUNNING, speed, simulationTime, wallTime);
    }

    public SimulationState stop(
            long expectedRevision, Instant currentSimulationTime, Instant wallTime) {
        requireRevision(expectedRevision);
        if (status != SimulationStatus.RUNNING && status != SimulationStatus.PAUSED) {
            throw new IllegalSimulationTransitionException(status, "stop");
        }
        return next(SimulationStatus.STOPPING, speed, currentSimulationTime, wallTime);
    }

    public SimulationState markStopped(long expectedRevision, Instant wallTime) {
        requireRevision(expectedRevision);
        requireStatus(SimulationStatus.STOPPING, "mark as stopped");
        return next(SimulationStatus.STOPPED, speed, simulationTime, wallTime);
    }

    public SimulationState changeSpeed(
            long expectedRevision,
            SimulationSpeed newSpeed,
            Instant currentSimulationTime,
            Instant wallTime) {
        requireRevision(expectedRevision);
        Objects.requireNonNull(newSpeed, "new simulation speed must not be null");
        if (status != SimulationStatus.RUNNING && status != SimulationStatus.PAUSED) {
            throw new IllegalSimulationTransitionException(status, "change speed of");
        }
        return next(status, newSpeed, currentSimulationTime, wallTime);
    }

    public SimulationState reset(
            long expectedRevision,
            String confirmation,
            long newSeed,
            Instant newSimulationTime,
            Instant wallTime) {
        requireRevision(expectedRevision);
        if (status != SimulationStatus.STOPPED || profile != SimulationProfile.DEMO) {
            throw new ResetNotAllowedException("Reset is allowed only for a stopped DEMO simulation");
        }
        if (!"RESET_DEMO".equals(confirmation)) {
            throw new ResetNotAllowedException("Reset requires explicit RESET_DEMO confirmation");
        }
        return new SimulationState(
                SimulationStatus.STOPPED,
                profile,
                newSeed,
                SimulationSpeed.X1,
                Objects.requireNonNull(newSimulationTime, "new simulation time must not be null"),
                Objects.requireNonNull(wallTime, "wall time must not be null"),
                revision + 1);
    }

    public SimulationState fail(long expectedRevision, Instant currentSimulationTime, Instant wallTime) {
        requireRevision(expectedRevision);
        if (status == SimulationStatus.STOPPED || status == SimulationStatus.FAILED) {
            throw new IllegalSimulationTransitionException(status, "fail");
        }
        return next(SimulationStatus.FAILED, speed, currentSimulationTime, wallTime);
    }

    private SimulationState next(
            SimulationStatus nextStatus,
            SimulationSpeed nextSpeed,
            Instant nextSimulationTime,
            Instant nextWallAnchor) {
        return new SimulationState(
                nextStatus,
                profile,
                seed,
                nextSpeed,
                Objects.requireNonNull(nextSimulationTime, "simulation time must not be null"),
                Objects.requireNonNull(nextWallAnchor, "wall time must not be null"),
                revision + 1);
    }

    private void requireRevision(long expectedRevision) {
        if (revision != expectedRevision) {
            throw new StaleSimulationRevisionException(expectedRevision, revision);
        }
    }

    private void requireStatus(SimulationStatus required, String action) {
        if (status != required) {
            throw new IllegalSimulationTransitionException(status, action);
        }
    }
}
