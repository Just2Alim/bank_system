package kz.sim.bank.simulation.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import kz.sim.bank.simulation.domain.FaultType;

public record ActivateFaultRequest(
        @NotNull FaultType type,
        @NotBlank @Pattern(regexp = "^[a-z0-9][a-z0-9-]{2,79}$") String targetService,
        @Min(1) @Max(900) int ttlSeconds) {}
