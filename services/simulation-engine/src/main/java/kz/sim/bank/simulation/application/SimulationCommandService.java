package kz.sim.bank.simulation.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import kz.sim.bank.simulation.domain.SimulationState;
import kz.sim.bank.simulation.domain.VirtualBusinessClock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SimulationCommandService {

    private static final String STATE_CHANGED = "simulation.state.changed";

    private final SimulationStateRepository repository;
    private final SimulationOutbox outbox;
    private final Clock wallClock;
    private final VirtualBusinessClock businessClock;

    public SimulationCommandService(
            SimulationStateRepository repository,
            SimulationOutbox outbox,
            Clock wallClock) {
        this.repository = Objects.requireNonNull(repository, "state repository must not be null");
        this.outbox = Objects.requireNonNull(outbox, "outbox must not be null");
        this.wallClock = Objects.requireNonNull(wallClock, "wall clock must not be null");
        this.businessClock = new VirtualBusinessClock(wallClock);
    }

    @Transactional
    public SimulationState execute(long expectedRevision, SimulationCommand command) {
        Objects.requireNonNull(command, "simulation command must not be null");
        SimulationState current = repository.findForUpdate()
                .orElseThrow(SimulationStateNotInitializedException::new);
        return switch (command.action()) {
            case START -> start(current, expectedRevision);
            case PAUSE -> persist(current, current.pause(
                    expectedRevision, businessClock.now(current), wallClock.instant()));
            case RESUME -> persist(current, current.resume(expectedRevision, wallClock.instant()));
            case STOP -> stop(current, expectedRevision);
            case SET_SPEED -> persist(current, current.changeSpeed(
                    expectedRevision,
                    command.speed().orElseThrow(() -> invalid("speed is required for SET_SPEED")),
                    businessClock.now(current),
                    wallClock.instant()));
            case RESET -> persist(current, current.reset(
                    expectedRevision,
                    command.confirmation().orElseThrow(() -> invalid("confirmation is required for RESET")),
                    command.seed().orElseThrow(() -> invalid("seed is required for RESET")),
                    command.simulationTime().orElseThrow(() -> invalid("simulationTime is required for RESET")),
                    wallClock.instant()));
        };
    }

    @Transactional(readOnly = true)
    public SimulationState current() {
        return repository.findCurrent().orElseThrow(SimulationStateNotInitializedException::new);
    }

    private SimulationState start(SimulationState current, long expectedRevision) {
        SimulationState starting = persist(current, current.start(expectedRevision, wallClock.instant()));
        return persist(starting, starting.markRunning(starting.revision(), wallClock.instant()));
    }

    private SimulationState stop(SimulationState current, long expectedRevision) {
        Instant businessTime = businessClock.now(current);
        SimulationState stopping = persist(
                current, current.stop(expectedRevision, businessTime, wallClock.instant()));
        return persist(stopping, stopping.markStopped(stopping.revision(), wallClock.instant()));
    }

    private SimulationState persist(SimulationState previous, SimulationState next) {
        SimulationState saved = repository.save(next);
        outbox.append(new SimulationEvent(
                STATE_CHANGED,
                saved.revision(),
                wallClock.instant(),
                saved.simulationTime(),
                previous.status(),
                saved.status(),
                saved.profile(),
                saved.speed()));
        return saved;
    }

    private static InvalidSimulationCommandException invalid(String message) {
        return new InvalidSimulationCommandException(message);
    }
}
