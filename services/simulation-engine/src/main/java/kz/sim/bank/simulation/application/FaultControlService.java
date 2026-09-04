package kz.sim.bank.simulation.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import kz.sim.bank.simulation.domain.FaultStatus;
import kz.sim.bank.simulation.domain.FaultType;
import kz.sim.bank.simulation.domain.SimulationFault;
import kz.sim.bank.simulation.domain.VirtualBusinessClock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FaultControlService {

    private final SimulationFaultRepository faults;
    private final SimulationStateRepository states;
    private final SimulationFaultOutbox outbox;
    private final Supplier<String> identifiers;
    private final Clock wallClock;

    public FaultControlService(
            SimulationFaultRepository faults,
            SimulationStateRepository states,
            SimulationFaultOutbox outbox,
            Supplier<String> identifiers,
            Clock wallClock) {
        this.faults = Objects.requireNonNull(faults, "fault repository must not be null");
        this.states = Objects.requireNonNull(states, "state repository must not be null");
        this.outbox = Objects.requireNonNull(outbox, "fault outbox must not be null");
        this.identifiers = Objects.requireNonNull(identifiers, "fault identifier supplier must not be null");
        this.wallClock = Objects.requireNonNull(wallClock, "wall clock must not be null");
    }

    @Transactional
    public SimulationFault activate(
            FaultType type, String targetService, int ttlSeconds, String actor) {
        Instant now = wallClock.instant();
        SimulationFault fault = SimulationFault.activate(
                identifiers.get(),
                type,
                targetService,
                now,
                Duration.ofSeconds(ttlSeconds),
                actor);
        SimulationFault saved = faults.save(fault);
        append(saved, "simulation.fault.activated", FaultStatus.ACTIVE, now);
        return saved;
    }

    @Transactional
    public SimulationFault clear(String faultId) {
        Instant now = wallClock.instant();
        SimulationFault current = faults.findForUpdate(faultId)
                .orElseThrow(() -> new FaultNotFoundException(faultId));
        SimulationFault cleared = faults.save(current.clear(now));
        append(cleared, "simulation.fault.cleared", FaultStatus.CLEARED, now);
        return cleared;
    }

    @Transactional(readOnly = true)
    public List<SimulationFault> active() {
        return List.copyOf(faults.findActive(wallClock.instant()));
    }

    private void append(
            SimulationFault fault, String eventType, FaultStatus status, Instant occurredAt) {
        var state = states.findCurrent().orElseThrow(SimulationStateNotInitializedException::new);
        Instant businessTime = new VirtualBusinessClock(wallClock).now(state);
        outbox.append(new SimulationFaultEvent(
                eventType,
                fault.id(),
                fault.revision(),
                occurredAt,
                businessTime,
                fault.type(),
                fault.targetService(),
                status,
                fault.expiresAt()));
    }
}
