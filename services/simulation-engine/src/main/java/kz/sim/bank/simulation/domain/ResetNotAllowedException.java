package kz.sim.bank.simulation.domain;

public final class ResetNotAllowedException extends RuntimeException {

    public ResetNotAllowedException(String message) {
        super(message);
    }
}
