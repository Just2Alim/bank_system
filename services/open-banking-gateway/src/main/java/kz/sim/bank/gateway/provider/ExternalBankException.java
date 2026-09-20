package kz.sim.bank.gateway.provider;

public class ExternalBankException extends RuntimeException {
    public ExternalBankException(String message) {
        super(message);
    }
}
