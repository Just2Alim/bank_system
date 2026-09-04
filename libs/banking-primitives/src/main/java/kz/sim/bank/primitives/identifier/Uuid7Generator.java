package kz.sim.bank.primitives.identifier;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.UUID;

final class Uuid7Generator {

    private static final SecureRandom RANDOM = new SecureRandom();

    private Uuid7Generator() {
    }

    static UUID next() {
        long timestamp = Clock.systemUTC().millis() & 0x0000_FFFF_FFFF_FFFFL;
        long randomA = RANDOM.nextInt(1 << 12);
        long mostSignificantBits = (timestamp << 16) | 0x7000L | randomA;
        long randomB = RANDOM.nextLong() & 0x3FFF_FFFF_FFFF_FFFFL;
        long leastSignificantBits = 0x8000_0000_0000_0000L | randomB;
        return new UUID(mostSignificantBits, leastSignificantBits);
    }
}
