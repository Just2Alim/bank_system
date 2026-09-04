package kz.sim.bank.simulation.application;

public final class SimulationStateNotInitializedException extends RuntimeException {

    public SimulationStateNotInitializedException() {
        super("Simulation state has not been initialized");
    }
}
