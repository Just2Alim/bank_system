package kz.sim.bank.simulation.api;

import java.time.Instant;

public record ApiSuccess<T>(boolean success, T data, Meta meta) {

    public static <T> ApiSuccess<T> strong(T data, long revision, Instant asOf) {
        return new ApiSuccess<>(true, data, new Meta(asOf, "STRONG", revision, 0L));
    }

    public record Meta(Instant asOf, String consistency, long revision, long lagMs) {}
}
