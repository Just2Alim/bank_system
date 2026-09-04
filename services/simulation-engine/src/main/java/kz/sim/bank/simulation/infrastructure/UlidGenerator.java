package kz.sim.bank.simulation.infrastructure;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Objects;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

@Component
public final class UlidGenerator implements Supplier<String> {

    private static final char[] CROCKFORD = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();
    private static final BigInteger BASE = BigInteger.valueOf(32L);
    private static final long MAX_TIMESTAMP = 0xFFFFFFFFFFFFL;

    private final Clock clock;
    private final SecureRandom random;

    public UlidGenerator(Clock clock) {
        this(clock, new SecureRandom());
    }

    UlidGenerator(Clock clock, SecureRandom random) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.random = Objects.requireNonNull(random, "random must not be null");
    }

    @Override
    public String get() {
        long timestamp = clock.millis();
        if (timestamp < 0 || timestamp > MAX_TIMESTAMP) {
            throw new IllegalStateException("Wall-clock value cannot be represented as ULID");
        }
        byte[] value = new byte[16];
        value[0] = (byte) (timestamp >>> 40);
        value[1] = (byte) (timestamp >>> 32);
        value[2] = (byte) (timestamp >>> 24);
        value[3] = (byte) (timestamp >>> 16);
        value[4] = (byte) (timestamp >>> 8);
        value[5] = (byte) timestamp;
        byte[] entropy = new byte[10];
        random.nextBytes(entropy);
        System.arraycopy(entropy, 0, value, 6, entropy.length);
        return "SIM-FLT-NPP-" + encode(value);
    }

    private static String encode(byte[] value) {
        BigInteger number = new BigInteger(1, value);
        char[] output = new char[26];
        for (int index = output.length - 1; index >= 0; index--) {
            BigInteger[] division = number.divideAndRemainder(BASE);
            output[index] = CROCKFORD[division[1].intValue()];
            number = division[0];
        }
        return new String(output);
    }
}
