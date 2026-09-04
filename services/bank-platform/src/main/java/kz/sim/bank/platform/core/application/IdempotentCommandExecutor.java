package kz.sim.bank.platform.core.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.util.function.Supplier;

/** Executes one mutation for a caller/key/payload tuple and replays the durable response thereafter. */
public final class IdempotentCommandExecutor {

    private final CoreBankingStore store;
    private final RequestFingerprint fingerprint;
    private final ObjectMapper mapper;
    private final Clock clock;

    public IdempotentCommandExecutor(CoreBankingStore store, ObjectMapper mapper, Clock clock) {
        this.store = store;
        this.mapper = mapper;
        this.fingerprint = new RequestFingerprint(mapper);
        this.clock = clock;
    }

    public <T> T execute(
            CommandContext context,
            String operation,
            Object request,
            Class<T> responseType,
            Supplier<T> mutation) {
        String requestHash = fingerprint.of(request);
        store.lockIdempotency(context.actorId(), operation, context.idempotencyKey());
        var existing = store.findIdempotentResponse(context.actorId(), operation, context.idempotencyKey());
        if (existing.isPresent()) {
            if (!existing.get().requestHash().equals(requestHash)) {
                throw new IdempotencyConflictException(
                        "Idempotency key was already used with a different request payload");
            }
            return read(existing.get().responseJson(), responseType);
        }

        T result = mutation.get();
        store.saveIdempotentResponse(
                context.actorId(),
                operation,
                context.idempotencyKey(),
                requestHash,
                write(result),
                Instant.now(clock));
        return result;
    }

    private String write(Object response) {
        try {
            return mapper.writeValueAsString(response);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot persist idempotent response", exception);
        }
    }

    private <T> T read(String json, Class<T> type) {
        try {
            return mapper.readValue(json, type);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot restore idempotent response", exception);
        }
    }
}
