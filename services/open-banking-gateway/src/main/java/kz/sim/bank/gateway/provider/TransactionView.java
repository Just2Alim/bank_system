package kz.sim.bank.gateway.provider;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionView(
        String id,
        Instant createdAt,
        String description,
        String counterparty,
        BigDecimal amount,
        String currency,
        String direction,
        String rail,
        String status,
        String correlationId,
        String accountId,
        String reference) {}
