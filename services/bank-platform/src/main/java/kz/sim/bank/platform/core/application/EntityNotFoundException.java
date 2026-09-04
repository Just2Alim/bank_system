package kz.sim.bank.platform.core.application;

public final class EntityNotFoundException extends RuntimeException {

    public EntityNotFoundException(String message) {
        super(message);
    }
}
