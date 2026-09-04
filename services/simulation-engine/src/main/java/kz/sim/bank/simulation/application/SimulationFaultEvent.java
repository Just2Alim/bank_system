package kz.sim.bank.simulation.application;

import java.time.Instant;
import java.util.Objects;
import kz.sim.bank.simulation.domain.FaultStatus;
import kz.sim.bank.simulation.domain.FaultType;

public record SimulationFaultEvent(
        String eventType,
        String faultId,
        long aggregateVersion,
        Instant occurredAt,
        Instant simulationTime,
        FaultType faultType,
        String targetService,
        FaultStatus status,
        Instant expiresAt) {

    public SimulationFaultEvent {
        Objects.requireNonNull(eventType, "event type must not be null");
        Objects.requireNonNull(faultId, "fault id must not be null");
        Objects.requireNonNull(occurredAt, "event occurrence time must not be null");
        Objects.requireNonNull(simulationTime, "simulation time must not be null");
        Objects.requireNonNull(faultType, "fault type must not be null");
        Objects.requireNonNull(targetService, "target service must not be null");
        Objects.requireNonNull(status, "fault status must not be null");
        Objects.requireNonNull(expiresAt, "fault expiry must not be null");
    }
}
