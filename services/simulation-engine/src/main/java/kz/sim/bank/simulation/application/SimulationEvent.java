package kz.sim.bank.simulation.application;

import java.time.Instant;
import java.util.Objects;
import kz.sim.bank.simulation.domain.SimulationProfile;
import kz.sim.bank.simulation.domain.SimulationSpeed;
import kz.sim.bank.simulation.domain.SimulationStatus;

public record SimulationEvent(
        String eventType,
        long aggregateVersion,
        Instant occurredAt,
        Instant simulationTime,
        SimulationStatus previousStatus,
        SimulationStatus currentStatus,
        SimulationProfile profile,
        SimulationSpeed speed) {

    public SimulationEvent {
        Objects.requireNonNull(eventType, "event type must not be null");
        Objects.requireNonNull(occurredAt, "event occurrence time must not be null");
        Objects.requireNonNull(simulationTime, "event simulation time must not be null");
        Objects.requireNonNull(previousStatus, "previous status must not be null");
        Objects.requireNonNull(currentStatus, "current status must not be null");
        Objects.requireNonNull(profile, "profile must not be null");
        Objects.requireNonNull(speed, "speed must not be null");
    }
}
