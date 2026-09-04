package kz.sim.bank.simulation.domain;

public final class IllegalSimulationTransitionException extends RuntimeException {

    public IllegalSimulationTransitionException(SimulationStatus current, String action) {
        super("Cannot " + action + " simulation while state is " + current);
    }
}
