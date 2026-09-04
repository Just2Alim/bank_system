package kz.sim.bank.platform.core.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Produces a stable, non-secret payload fingerprint for idempotency conflict detection. */
public final class RequestFingerprint {

    private final ObjectMapper canonicalMapper;

    public RequestFingerprint(ObjectMapper mapper) {
        if (mapper == null) {
            throw new IllegalArgumentException("object mapper must not be null");
        }
        this.canonicalMapper = mapper.copy().enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY);
    }

    public String of(Object payload) {
        if (payload == null) {
            throw new IllegalArgumentException("request payload must not be null");
        }
        try {
            byte[] canonicalJson = canonicalMapper.writeValueAsString(payload).getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonicalJson));
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("request payload cannot be serialized", exception);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }
}
