package kz.sim.bank.simulation.application;

public final class FaultNotFoundException extends RuntimeException {

    public FaultNotFoundException(String faultId) {
        super("Simulation fault was not found: " + faultId);
    }
}
