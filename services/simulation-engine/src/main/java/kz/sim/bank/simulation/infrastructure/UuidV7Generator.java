package kz.sim.bank.simulation.infrastructure;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public final class UuidV7Generator {

    private static final long MAX_UNIX_MILLIS = 0xFFFFFFFFFFFFL;
    private static final long VARIANT_MASK = 0x3FFFFFFFFFFFFFFFL;
    private static final long RFC_4122_VARIANT = 0x8000000000000000L;

    private final Clock clock;
    private final SecureRandom random;

    public UuidV7Generator(Clock clock) {
        this(clock, new SecureRandom());
    }

    UuidV7Generator(Clock clock, SecureRandom random) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.random = Objects.requireNonNull(random, "random must not be null");
    }

    public UUID next() {
        long timestamp = clock.millis();
        if (timestamp < 0 || timestamp > MAX_UNIX_MILLIS) {
            throw new IllegalStateException("Wall-clock value cannot be represented as UUIDv7");
        }
        long randomA = random.nextInt(1 << 12);
        long mostSignificant = (timestamp << 16) | 0x7000L | randomA;
        long leastSignificant = (random.nextLong() & VARIANT_MASK) | RFC_4122_VARIANT;
        return new UUID(mostSignificant, leastSignificant);
    }
}
