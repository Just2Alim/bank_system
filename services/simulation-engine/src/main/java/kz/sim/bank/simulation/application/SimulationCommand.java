package kz.sim.bank.simulation.application;

import java.time.Instant;
import java.util.Optional;
import kz.sim.bank.simulation.domain.SimulationSpeed;

public record SimulationCommand(
        SimulationAction action,
        Optional<SimulationSpeed> speed,
        Optional<String> confirmation,
        Optional<Long> seed,
        Optional<Instant> simulationTime) {

    public SimulationCommand {
        if (action == null) {
            throw new InvalidSimulationCommandException("Simulation action is required");
        }
        speed = speed == null ? Optional.empty() : speed;
        confirmation = confirmation == null ? Optional.empty() : confirmation;
        seed = seed == null ? Optional.empty() : seed;
        simulationTime = simulationTime == null ? Optional.empty() : simulationTime;
    }

    public static SimulationCommand start() {
        return of(SimulationAction.START);
    }

    public static SimulationCommand pause() {
        return of(SimulationAction.PAUSE);
    }

    public static SimulationCommand resume() {
        return of(SimulationAction.RESUME);
    }

    public static SimulationCommand stop() {
        return of(SimulationAction.STOP);
    }

    public static SimulationCommand setSpeed(SimulationSpeed speed) {
        return new SimulationCommand(
                SimulationAction.SET_SPEED,
                Optional.ofNullable(speed),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }

    public static SimulationCommand reset(String confirmation, long seed, Instant simulationTime) {
        return new SimulationCommand(
                SimulationAction.RESET,
                Optional.empty(),
                Optional.ofNullable(confirmation),
                Optional.of(seed),
                Optional.ofNullable(simulationTime));
    }

    private static SimulationCommand of(SimulationAction action) {
        return new SimulationCommand(
                action, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }
}
