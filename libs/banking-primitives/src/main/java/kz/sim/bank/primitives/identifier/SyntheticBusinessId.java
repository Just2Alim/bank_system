package kz.sim.bank.primitives.identifier;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Internal generator and validator for opaque synthetic business identifiers. */
final class SyntheticBusinessId {

    private static final char[] CROCKFORD_BASE32 = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();
    private static final Pattern ULID_PATTERN = Pattern.compile("[0-7][0-9A-HJKMNP-TV-Z]{25}");
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final BigInteger BASE = BigInteger.valueOf(32L);

    private SyntheticBusinessId() {
    }

    static String next(String type, BankCode bankCode) {
        Objects.requireNonNull(bankCode, "bank code must not be null");
        return "SIM-" + type + "-" + bankCode.value() + "-" + nextUlid(Clock.systemUTC().millis());
    }

    static String requireType(String value, String type) {
        Objects.requireNonNull(value, type + " id must not be null");
        Matcher matcher = patternFor(type).matcher(value);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    type + " id must match SIM-" + type + "-<BANKCODE>-<ULID>");
        }
        return value;
    }

    static BankCode bankCode(String value, String type) {
        Matcher matcher = patternFor(type).matcher(requireType(value, type));
        if (!matcher.matches()) {
            throw new IllegalStateException("Validated business identifier no longer matches its type");
        }
        return new BankCode(matcher.group(1));
    }

    private static Pattern patternFor(String type) {
        return Pattern.compile(
                "SIM-" + Pattern.quote(type) + "-([A-Z0-9]{2,12})-(" + ULID_PATTERN.pattern() + ")");
    }

    private static String nextUlid(long timestampMillis) {
        byte[] bytes = new byte[16];
        bytes[0] = (byte) (timestampMillis >>> 40);
        bytes[1] = (byte) (timestampMillis >>> 32);
        bytes[2] = (byte) (timestampMillis >>> 24);
        bytes[3] = (byte) (timestampMillis >>> 16);
        bytes[4] = (byte) (timestampMillis >>> 8);
        bytes[5] = (byte) timestampMillis;
        byte[] randomness = new byte[10];
        RANDOM.nextBytes(randomness);
        System.arraycopy(randomness, 0, bytes, 6, randomness.length);

        BigInteger remaining = new BigInteger(1, bytes);
        char[] encoded = new char[26];
        for (int index = encoded.length - 1; index >= 0; index--) {
            BigInteger[] division = remaining.divideAndRemainder(BASE);
            encoded[index] = CROCKFORD_BASE32[division[1].intValue()];
            remaining = division[0];
        }
        return new String(encoded);
    }
}
