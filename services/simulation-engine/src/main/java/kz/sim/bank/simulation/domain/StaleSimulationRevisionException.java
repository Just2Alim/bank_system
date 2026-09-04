package kz.sim.bank.simulation.domain;

public final class StaleSimulationRevisionException extends RuntimeException {

    public StaleSimulationRevisionException(long expected, long actual) {
        super("Expected simulation revision " + expected + " but current revision is " + actual);
    }
}
