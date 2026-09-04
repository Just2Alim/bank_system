package kz.sim.bank.simulation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import kz.sim.bank.simulation.domain.FaultStatus;
import kz.sim.bank.simulation.domain.FaultType;
import kz.sim.bank.simulation.domain.SimulationFault;
import kz.sim.bank.simulation.domain.SimulationProfile;
import kz.sim.bank.simulation.domain.SimulationState;
import org.junit.jupiter.api.Test;

class FaultControlServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-04T10:00:00Z");
    private static final String ID = "SIM-FLT-NPP-01K4A2X9V7A8Z5N3C1M6Q0R2TB";

    @Test
    void activationAndClearArePersistedAndEmittedExactlyOnce() {
        FaultMemory faults = new FaultMemory();
        EventMemory events = new EventMemory();
        FaultControlService service = service(faults, events);

        SimulationFault active = service.activate(
                FaultType.BANK_UNAVAILABLE, "bank-platform-orda", 30, "operator-001");
        SimulationFault cleared = service.clear(active.id());

        assertThat(cleared.statusAt(NOW)).isEqualTo(FaultStatus.CLEARED);
        assertThat(events.values)
                .extracting(SimulationFaultEvent::eventType)
                .containsExactly("simulation.fault.activated", "simulation.fault.cleared");
        assertThat(faults.values).containsEntry(ID, cleared);
    }

    @Test
    void unknownFaultCannotBeCleared() {
        FaultControlService service = service(new FaultMemory(), new EventMemory());

        assertThatThrownBy(() -> service.clear(ID))
                .isInstanceOf(FaultNotFoundException.class);
    }

    private static FaultControlService service(FaultMemory faults, EventMemory events) {
        SimulationState initial = SimulationState.initial(
                SimulationProfile.DEMO, 42L, NOW.minusSeconds(100), NOW);
        SimulationStateRepository states = new SimulationStateRepository() {
            @Override
            public Optional<SimulationState> findForUpdate() {
                return Optional.of(initial);
            }

            @Override
            public SimulationState save(SimulationState state) {
                return state;
            }
        };
        return new FaultControlService(
                faults,
                states,
                events,
                () -> ID,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static final class FaultMemory implements SimulationFaultRepository {
        private final Map<String, SimulationFault> values = new LinkedHashMap<>();

        @Override
        public SimulationFault save(SimulationFault fault) {
            values.put(fault.id(), fault);
            return fault;
        }

        @Override
        public Optional<SimulationFault> findForUpdate(String faultId) {
            return Optional.ofNullable(values.get(faultId));
        }

        @Override
        public List<SimulationFault> findActive(Instant wallTime) {
            return values.values().stream()
                    .filter(fault -> fault.statusAt(wallTime) == FaultStatus.ACTIVE)
                    .toList();
        }
    }

    private static final class EventMemory implements SimulationFaultOutbox {
        private final List<SimulationFaultEvent> values = new ArrayList<>();

        @Override
        public void append(SimulationFaultEvent event) {
            values.add(event);
        }
    }
}
