package kz.sim.bank.platform.core.domain;

public final class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(String message) {
        super(message);
    }
}
