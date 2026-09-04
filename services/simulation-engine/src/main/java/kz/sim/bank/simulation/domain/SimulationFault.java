package kz.sim.bank.simulation.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/** A deliberately bounded availability/delay control, measured only by wall clock. */
public record SimulationFault(
        String id,
        FaultType type,
        String targetService,
        Instant activatedAt,
        Instant expiresAt,
        Optional<Instant> clearedAt,
        String activatedBy,
        long revision) {

    private static final Pattern ID_PATTERN =
            Pattern.compile("^SIM-FLT-NPP-[0-9A-HJKMNP-TV-Z]{26}$");
    private static final Pattern TARGET_PATTERN = Pattern.compile("^[a-z0-9][a-z0-9-]{2,79}$");
    private static final Duration MIN_TTL = Duration.ofSeconds(1);
    private static final Duration MAX_TTL = Duration.ofSeconds(900);

    public SimulationFault {
        Objects.requireNonNull(id, "fault id must not be null");
        Objects.requireNonNull(type, "fault type must not be null");
        Objects.requireNonNull(targetService, "target service must not be null");
        Objects.requireNonNull(activatedAt, "activation time must not be null");
        Objects.requireNonNull(expiresAt, "expiry time must not be null");
        Objects.requireNonNull(clearedAt, "cleared time must not be null");
        Objects.requireNonNull(activatedBy, "actor must not be null");
        if (!ID_PATTERN.matcher(id).matches()) {
            throw new IllegalArgumentException("Fault id must use the SIM-FLT-NPP ULID namespace");
        }
        if (!TARGET_PATTERN.matcher(targetService).matches()) {
            throw new IllegalArgumentException("Fault target service is invalid");
        }
        if (activatedBy.isBlank() || activatedBy.length() > 128) {
            throw new IllegalArgumentException("Fault actor must contain 1 to 128 characters");
        }
        Duration ttl = Duration.between(activatedAt, expiresAt);
        if (ttl.compareTo(MIN_TTL) < 0 || ttl.compareTo(MAX_TTL) > 0) {
            throw new IllegalArgumentException("Fault TTL must be between 1 and 900 seconds");
        }
        if (revision < 1) {
            throw new IllegalArgumentException("Fault revision must be positive");
        }
        clearedAt.ifPresent(cleared -> {
            if (cleared.isBefore(activatedAt) || !cleared.isBefore(expiresAt)) {
                throw new IllegalArgumentException("Fault clear time must be within the active interval");
            }
        });
    }

    public static SimulationFault activate(
            String id,
            FaultType type,
            String targetService,
            Instant activatedAt,
            Duration ttl,
            String activatedBy) {
        Objects.requireNonNull(ttl, "fault TTL must not be null");
        return new SimulationFault(
                id,
                type,
                targetService,
                activatedAt,
                activatedAt.plus(ttl),
                Optional.empty(),
                activatedBy,
                1L);
    }

    public SimulationFault clear(Instant at) {
        Objects.requireNonNull(at, "clear time must not be null");
        if (statusAt(at) != FaultStatus.ACTIVE) {
            throw new IllegalStateException("Only an active fault can be cleared");
        }
        return new SimulationFault(
                id, type, targetService, activatedAt, expiresAt, Optional.of(at), activatedBy, revision + 1);
    }

    public FaultStatus statusAt(Instant at) {
        Objects.requireNonNull(at, "status time must not be null");
        if (clearedAt.isPresent()) {
            return FaultStatus.CLEARED;
        }
        return at.isBefore(expiresAt) ? FaultStatus.ACTIVE : FaultStatus.EXPIRED;
    }
}
