package com.ukg.shiftswap.web;

import com.ukg.shiftswap.domain.SwapRequestStatus;
import com.ukg.shiftswap.domain.exception.ForbiddenException;
import com.ukg.shiftswap.domain.exception.InvalidStateTransitionException;
import com.ukg.shiftswap.domain.exception.NotFoundException;
import com.ukg.shiftswap.domain.exception.RequestExpiredException;
import com.ukg.shiftswap.domain.exception.ShiftAlreadyInRequestException;
import com.ukg.shiftswap.domain.exception.SwapRuleViolationException;
import com.ukg.shiftswap.domain.exception.ValidationException;
import com.ukg.shiftswap.service.SwapMetrics;
import com.ukg.shiftswap.web.error.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

/**
 * The single mapping point from every domain exception.
 * No endpoint builds its own error response.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final SwapMetrics metrics;

    public GlobalExceptionHandler(SwapMetrics metrics) {
        this.metrics = metrics;
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.code(), ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleBeanValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + " " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message.isBlank() ? "Validation failed" : message, request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "Invalid value for '" + ex.getName() + "': " + ex.getValue(), request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Malformed request body", request);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, ex.code(), ex.getMessage(), request);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.code(), ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidStateTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTransition(InvalidStateTransitionException ex, HttpServletRequest request) {
        metrics.recordConflict(ex.code());
        ErrorResponse body = ErrorResponse.withResolution(HttpStatus.CONFLICT, ex.code(), ex.getMessage(),
                request.getRequestURI(), ex.currentStatus().name(), ex.resolvedBy());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(RequestExpiredException.class)
    public ResponseEntity<ErrorResponse> handleExpired(RequestExpiredException ex, HttpServletRequest request) {
        metrics.recordConflict(ex.code());
        ErrorResponse body = ErrorResponse.withResolution(HttpStatus.CONFLICT, ex.code(), ex.getMessage(),
                request.getRequestURI(), SwapRequestStatus.EXPIRED.name(), null);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(ShiftAlreadyInRequestException.class)
    public ResponseEntity<ErrorResponse> handleShiftAlreadyInRequest(ShiftAlreadyInRequestException ex, HttpServletRequest request) {
        metrics.recordConflict(ex.code());
        return build(HttpStatus.CONFLICT, ex.code(), ex.getMessage(), request);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(ObjectOptimisticLockingFailureException ex, HttpServletRequest request) {
        metrics.recordConflict("CONCURRENT_MODIFICATION");
        return build(HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION",
                "This resource was modified concurrently; reload and retry", request);
    }

    @ExceptionHandler(SwapRuleViolationException.class)
    public ResponseEntity<ErrorResponse> handleRuleViolation(SwapRuleViolationException ex, HttpServletRequest request) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.code(), ex.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred", request);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String code, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(ErrorResponse.of(status, code, message, request.getRequestURI()));
    }
}
