package kz.sim.bank.simulation.api;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Optional;
import kz.sim.bank.simulation.application.SimulationAction;
import kz.sim.bank.simulation.application.SimulationCommand;
import kz.sim.bank.simulation.domain.SimulationSpeed;

public record SimulationCommandRequest(
        @NotNull SimulationAction action,
        Integer speed,
        String confirmation,
        Long seed,
        Instant simulationTime) {

    SimulationCommand toCommand() {
        Optional<SimulationSpeed> mappedSpeed = Optional.ofNullable(speed)
                .map(SimulationSpeed::fromMultiplier);
        return new SimulationCommand(
                action,
                mappedSpeed,
                Optional.ofNullable(confirmation),
                Optional.ofNullable(seed),
                Optional.ofNullable(simulationTime));
    }
}
