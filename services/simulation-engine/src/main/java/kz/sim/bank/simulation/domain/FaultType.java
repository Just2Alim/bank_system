package kz.sim.bank.simulation.domain;

public enum FaultType {
    BANK_UNAVAILABLE,
    KAFKA_CONSUMER_DELAY,
    NPP_UNAVAILABLE,
    RTGS_QUEUE_DELAY,
    NOTIFICATION_UNAVAILABLE
}
