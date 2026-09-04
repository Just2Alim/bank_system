package kz.sim.bank.simulation.api;

import jakarta.validation.Valid;
import java.time.Clock;
import java.util.Objects;
import kz.sim.bank.simulation.application.InvalidSimulationCommandException;
import kz.sim.bank.simulation.application.SimulationCommandService;
import kz.sim.bank.simulation.domain.SimulationState;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/simulation")
public class SimulationController {

    private final SimulationCommandService commands;
    private final Clock wallClock;

    public SimulationController(SimulationCommandService commands, Clock wallClock) {
        this.commands = Objects.requireNonNull(commands, "command service must not be null");
        this.wallClock = Objects.requireNonNull(wallClock, "wall clock must not be null");
    }

    @GetMapping("/state")
    public ResponseEntity<ApiSuccess<SimulationState>> state() {
        SimulationState state = commands.current();
        return response(state);
    }

    @PostMapping("/commands")
    public ResponseEntity<ApiSuccess<SimulationState>> command(
            @RequestHeader("If-Match") String ifMatch,
            @Valid @RequestBody SimulationCommandRequest request) {
        SimulationState state = commands.execute(parseRevision(ifMatch), request.toCommand());
        return response(state);
    }

    private ResponseEntity<ApiSuccess<SimulationState>> response(SimulationState state) {
        return ResponseEntity.ok()
                .eTag(Long.toString(state.revision()))
                .body(ApiSuccess.strong(state, state.revision(), wallClock.instant()));
    }

    static long parseRevision(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidSimulationCommandException("If-Match revision is required");
        }
        String normalized = value.strip();
        if (normalized.startsWith("W/")) {
            normalized = normalized.substring(2);
        }
        if (normalized.length() >= 2 && normalized.startsWith("\"") && normalized.endsWith("\"")) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        try {
            long revision = Long.parseLong(normalized);
            if (revision < 0) {
                throw new NumberFormatException("negative revision");
            }
            return revision;
        } catch (NumberFormatException exception) {
            throw new InvalidSimulationCommandException("If-Match must contain a non-negative revision");
        }
    }
}
