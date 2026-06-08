package com.workmate.interfaces.rest;

import com.workmate.application.common.ResourceNotFoundException;
import com.workmate.domain.common.DomainException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.time.Instant;

/**
 * Global exception handler for the REST interface layer.
 *
 * <p>Maps application and domain exceptions to structured HTTP error responses.
 * All error bodies use the {@link ErrorResponse} record.
 */
@RestControllerAdvice
public class RestExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);

    /**
     * Structured error response body.
     *
     * @param error     short machine-readable error type (e.g. "DOMAIN_ERROR")
     * @param message   human-readable description of what went wrong
     * @param timestamp when the error occurred
     */
    public record ErrorResponse(String error, String message, Instant timestamp) {}

    /**
     * Maps domain invariant violations to 400 Bad Request.
     */
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomainException(DomainException ex) {
        log.debug("Domain exception: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("DOMAIN_ERROR", ex.getMessage(), Instant.now()));
    }

    /**
     * Maps resource-not-found conditions to 404 Not Found.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex) {
        log.debug("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("NOT_FOUND", ex.getMessage(), Instant.now()));
    }

    /**
     * Maps bean-validation failures (WebFlux) to 400 Bad Request.
     */
    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ErrorResponse> handleWebExchangeBindException(
            WebExchangeBindException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + " " + fe.getDefaultMessage())
                .findFirst()
                .orElse(ex.getMessage());
        log.debug("Validation error: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("VALIDATION_ERROR", message, Instant.now()));
    }

    /**
     * Maps Spring MVC bean-validation failures to 400 Bad Request.
     * Included for completeness; WebFlux primarily uses {@link WebExchangeBindException}.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + " " + fe.getDefaultMessage())
                .findFirst()
                .orElse(ex.getMessage());
        log.debug("Validation error: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("VALIDATION_ERROR", message, Instant.now()));
    }

    /**
     * Catch-all handler for unexpected errors — returns 500 Internal Server Error.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_ERROR",
                        "An unexpected error occurred", Instant.now()));
    }
}
