package kz.sim.bank.simulation.application;

import java.util.Optional;
import kz.sim.bank.simulation.domain.SimulationState;

public interface SimulationStateRepository {

    Optional<SimulationState> findForUpdate();

    default Optional<SimulationState> findCurrent() {
        return findForUpdate();
    }

    SimulationState save(SimulationState state);
}
