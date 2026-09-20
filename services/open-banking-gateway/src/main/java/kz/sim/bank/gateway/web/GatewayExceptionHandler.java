package kz.sim.bank.gateway.web;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GatewayExceptionHandler {
    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
    ResponseEntity<Map<String, Object>> badRequest(Exception exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", Map.of("code", "INVALID_REQUEST", "message", exception.getMessage())));
    }
}
