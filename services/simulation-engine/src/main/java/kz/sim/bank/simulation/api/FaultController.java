package kz.sim.bank.simulation.api;

import jakarta.validation.Valid;
import java.security.Principal;
import java.time.Clock;
import java.util.List;
import kz.sim.bank.simulation.application.FaultControlService;
import kz.sim.bank.simulation.domain.SimulationFault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/simulation/faults")
public class FaultController {

    private final FaultControlService service;
    private final Clock clock;

    public FaultController(FaultControlService service, Clock clock) {
        this.service = service;
        this.clock = clock;
    }

    @GetMapping
    public ApiSuccess<List<SimulationFault>> active() {
        List<SimulationFault> faults = service.active();
        return ApiSuccess.strong(faults, 0L, clock.instant());
    }

    @PostMapping
    public ResponseEntity<ApiSuccess<SimulationFault>> activate(
            @Valid @RequestBody ActivateFaultRequest request, Principal principal) {
        SimulationFault fault = service.activate(
                request.type(), request.targetService(), request.ttlSeconds(), principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .eTag(Long.toString(fault.revision()))
                .body(ApiSuccess.strong(fault, fault.revision(), clock.instant()));
    }

    @PostMapping("/{faultId}/clear")
    public ResponseEntity<ApiSuccess<SimulationFault>> clear(@PathVariable String faultId) {
        SimulationFault fault = service.clear(faultId);
        return ResponseEntity.ok()
                .eTag(Long.toString(fault.revision()))
                .body(ApiSuccess.strong(fault, fault.revision(), clock.instant()));
    }
}
