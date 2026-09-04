package kz.sim.bank.simulation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import kz.sim.bank.simulation.domain.SimulationProfile;
import kz.sim.bank.simulation.domain.SimulationSpeed;
import kz.sim.bank.simulation.domain.SimulationState;
import kz.sim.bank.simulation.domain.SimulationStatus;
import org.junit.jupiter.api.Test;

class SimulationCommandServiceTest {

    private static final Instant WALL_TIME = Instant.parse("2026-09-04T10:00:00Z");
    private static final Instant BUSINESS_TIME = Instant.parse("2026-09-01T00:00:00Z");

    @Test
    void startPersistsRunningStateAndBothObservableTransitions() {
        InMemoryStateRepository states = new InMemoryStateRepository(initial());
        RecordingOutbox outbox = new RecordingOutbox();
        SimulationCommandService service = service(states, outbox);

        SimulationState result = service.execute(0L, SimulationCommand.start());

        assertThat(result.status()).isEqualTo(SimulationStatus.RUNNING);
        assertThat(result.revision()).isEqualTo(2L);
        assertThat(states.current).isEqualTo(result);
        assertThat(outbox.events)
                .extracting(SimulationEvent::eventType)
                .containsExactly("simulation.state.changed", "simulation.state.changed");
        assertThat(outbox.events)
                .extracting(SimulationEvent::aggregateVersion)
                .containsExactly(1L, 2L);
    }

    @Test
    void pauseFreezesTheCalculatedAcceleratedBusinessTime() {
        SimulationState running = initial()
                .start(0L, WALL_TIME.minusSeconds(11))
                .markRunning(1L, WALL_TIME.minusSeconds(10))
                .changeSpeed(2L, SimulationSpeed.X60, BUSINESS_TIME, WALL_TIME.minusSeconds(10));
        InMemoryStateRepository states = new InMemoryStateRepository(running);
        SimulationCommandService service = service(states, new RecordingOutbox());

        SimulationState paused = service.execute(3L, SimulationCommand.pause());

        assertThat(paused.status()).isEqualTo(SimulationStatus.PAUSED);
        assertThat(paused.simulationTime()).isEqualTo(BUSINESS_TIME.plusSeconds(600));
    }

    @Test
    void validatesCommandSpecificParametersBeforeChangingState() {
        InMemoryStateRepository states = new InMemoryStateRepository(initial());
        SimulationCommandService service = service(states, new RecordingOutbox());

        assertThatThrownBy(() -> service.execute(0L, SimulationCommand.setSpeed(null)))
                .isInstanceOf(InvalidSimulationCommandException.class);
        assertThat(states.current).isEqualTo(initial());
    }

    private static SimulationCommandService service(
            SimulationStateRepository repository, SimulationOutbox outbox) {
        return new SimulationCommandService(
                repository,
                outbox,
                Clock.fixed(WALL_TIME, ZoneOffset.UTC));
    }

    private static SimulationState initial() {
        return SimulationState.initial(SimulationProfile.DEMO, 42L, BUSINESS_TIME, WALL_TIME);
    }

    private static final class InMemoryStateRepository implements SimulationStateRepository {
        private SimulationState current;

        private InMemoryStateRepository(SimulationState current) {
            this.current = current;
        }

        @Override
        public Optional<SimulationState> findForUpdate() {
            return Optional.of(current);
        }

        @Override
        public SimulationState save(SimulationState state) {
            current = state;
            return state;
        }
    }

    private static final class RecordingOutbox implements SimulationOutbox {
        private final List<SimulationEvent> events = new ArrayList<>();

        @Override
        public void append(SimulationEvent event) {
            events.add(event);
        }
    }
}
