package kz.sim.bank.gateway.provider;

import java.time.Instant;

public record TransferReceipt(String commandId, String resourceId, String status, Instant bookedAt) {}
