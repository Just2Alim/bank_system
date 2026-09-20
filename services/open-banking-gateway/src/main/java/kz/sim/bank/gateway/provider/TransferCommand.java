package kz.sim.bank.gateway.provider;

import java.math.BigDecimal;

public record TransferCommand(
        String idempotencyKey,
        String sourceAccountId,
        String destinationAccountId,
        BigDecimal amount,
        String purpose) {}
