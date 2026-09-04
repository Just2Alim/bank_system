package kz.sim.bank.simulation.api;

import java.time.Clock;
import java.time.Instant;
import kz.sim.bank.simulation.application.InvalidSimulationCommandException;
import kz.sim.bank.simulation.application.FaultNotFoundException;
import kz.sim.bank.simulation.application.SimulationStateNotInitializedException;
import kz.sim.bank.simulation.domain.IllegalSimulationTransitionException;
import kz.sim.bank.simulation.domain.ResetNotAllowedException;
import kz.sim.bank.simulation.domain.StaleSimulationRevisionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    private final Clock clock;

    public ApiExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler({InvalidSimulationCommandException.class, MethodArgumentNotValidException.class})
    ResponseEntity<ApiFailure> badRequest(Exception exception) {
        return failure(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", safeMessage(exception));
    }

    @ExceptionHandler(StaleSimulationRevisionException.class)
    ResponseEntity<ApiFailure> stale(StaleSimulationRevisionException exception) {
        return failure(HttpStatus.PRECONDITION_FAILED, "CONCURRENT_MODIFICATION", exception.getMessage());
    }

    @ExceptionHandler(IllegalSimulationTransitionException.class)
    ResponseEntity<ApiFailure> conflict(IllegalSimulationTransitionException exception) {
        return failure(HttpStatus.CONFLICT, "ILLEGAL_STATE_TRANSITION", exception.getMessage());
    }

    @ExceptionHandler(ResetNotAllowedException.class)
    ResponseEntity<ApiFailure> reset(ResetNotAllowedException exception) {
        return failure(HttpStatus.UNPROCESSABLE_ENTITY, "RESET_NOT_ALLOWED", exception.getMessage());
    }

    @ExceptionHandler(SimulationStateNotInitializedException.class)
    ResponseEntity<ApiFailure> unavailable(SimulationStateNotInitializedException exception) {
        return failure(HttpStatus.SERVICE_UNAVAILABLE, "DEPENDENCY_UNAVAILABLE", exception.getMessage());
    }

    @ExceptionHandler(FaultNotFoundException.class)
    ResponseEntity<ApiFailure> notFound(FaultNotFoundException exception) {
        return failure(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", exception.getMessage());
    }

    private ResponseEntity<ApiFailure> failure(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status)
                .body(new ApiFailure(false, null, code, message, clock.instant()));
    }

    private static String safeMessage(Exception exception) {
        if (exception instanceof MethodArgumentNotValidException) {
            return "Request validation failed";
        }
        return exception.getMessage();
    }

    public record ApiFailure(
            boolean success, Object data, String code, String message, Instant timestamp) {}
}
