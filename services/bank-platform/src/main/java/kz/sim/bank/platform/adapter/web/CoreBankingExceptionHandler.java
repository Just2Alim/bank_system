package kz.sim.bank.platform.adapter.web;

import java.time.Instant;
import kz.sim.bank.platform.core.application.EntityNotFoundException;
import kz.sim.bank.platform.core.application.IdempotencyConflictException;
import kz.sim.bank.platform.core.domain.InsufficientFundsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = CoreBankingController.class)
public class CoreBankingExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorResponse> invalidRequest() {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request validation failed");
    }

    @ExceptionHandler({IllegalArgumentException.class, InsufficientFundsException.class})
    ResponseEntity<ApiErrorResponse> unprocessable(RuntimeException exception) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, "COMMAND_REJECTED", exception.getMessage());
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    ResponseEntity<ApiErrorResponse> idempotencyConflict(IdempotencyConflictException exception) {
        return error(HttpStatus.CONFLICT, "IDEMPOTENCY_CONFLICT", exception.getMessage());
    }

    @ExceptionHandler(EntityNotFoundException.class)
    ResponseEntity<ApiErrorResponse> notFound(EntityNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", exception.getMessage());
    }

    private static ResponseEntity<ApiErrorResponse> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status)
                .body(new ApiErrorResponse(new ErrorBody(code, message), Instant.now()));
    }

    public record ApiErrorResponse(ErrorBody error, Instant timestamp) {}

    public record ErrorBody(String code, String message) {}
}
