package kz.sim.bank.simulation.application;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import kz.sim.bank.simulation.domain.SimulationFault;

public interface SimulationFaultRepository {

    SimulationFault save(SimulationFault fault);

    Optional<SimulationFault> findForUpdate(String faultId);

    List<SimulationFault> findActive(Instant wallTime);
}
