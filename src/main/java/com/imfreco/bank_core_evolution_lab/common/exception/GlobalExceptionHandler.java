package com.imfreco.bank_core_evolution_lab.common.exception;

import com.imfreco.bank_core_evolution_lab.common.logging.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException exception, HttpServletRequest request) {
        List<FieldErrorDetail> details =
                exception.getBindingResult().getFieldErrors().stream()
                        .map(
                                error ->
                                        new FieldErrorDetail(
                                                error.getField(), error.getDefaultMessage()))
                        .toList();
        return build(
                HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Invalid request", request, details);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    ResponseEntity<ErrorResponse> handleMissingHeader(
            MissingRequestHeaderException exception, HttpServletRequest request) {
        return build(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "Missing required header: " + exception.getHeaderName(),
                request,
                List.of());
    }

    @ExceptionHandler({
        AccountNotFoundException.class,
        CustomerNotFoundException.class,
        TransferNotFoundException.class
    })
    ResponseEntity<ErrorResponse> handleNotFound(
            DomainException exception, HttpServletRequest request) {
        return build(
                HttpStatus.NOT_FOUND,
                exception.errorCode(),
                exception.getMessage(),
                request,
                List.of());
    }

    @ExceptionHandler({DuplicateIdempotencyKeyException.class, DuplicateUsernameException.class})
    ResponseEntity<ErrorResponse> handleDuplicate(
            DomainException exception, HttpServletRequest request) {
        return build(
                HttpStatus.CONFLICT,
                exception.errorCode(),
                exception.getMessage(),
                request,
                List.of());
    }

    @ExceptionHandler(AuthenticationFailedException.class)
    ResponseEntity<ErrorResponse> handleAuthentication(
            AuthenticationFailedException exception, HttpServletRequest request) {
        return build(
                HttpStatus.UNAUTHORIZED,
                exception.errorCode(),
                exception.getMessage(),
                request,
                List.of());
    }

    @ExceptionHandler({AccessDeniedException.class, ForbiddenOperationException.class})
    ResponseEntity<ErrorResponse> handleAccessDenied(
            RuntimeException exception, HttpServletRequest request) {
        return build(
                HttpStatus.FORBIDDEN,
                "FORBIDDEN",
                "You are not allowed to access this resource",
                request,
                List.of());
    }

    @ExceptionHandler({
        AccountBlockedException.class,
        CurrencyMismatchException.class,
        InsufficientFundsException.class,
        InvalidTransferException.class
    })
    ResponseEntity<ErrorResponse> handleBusiness(
            DomainException exception, HttpServletRequest request) {
        return build(
                HttpStatus.UNPROCESSABLE_ENTITY,
                exception.errorCode(),
                exception.getMessage(),
                request,
                List.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleUnexpected(
            Exception exception, HttpServletRequest request) {
        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "Unexpected error processing request",
                request,
                List.of());
    }

    private ResponseEntity<ErrorResponse> build(
            HttpStatus status,
            String error,
            String message,
            HttpServletRequest request,
            List<FieldErrorDetail> details) {
        ErrorResponse response =
                new ErrorResponse(
                        Instant.now(),
                        status.value(),
                        error,
                        message,
                        request.getRequestURI(),
                        MDC.get(CorrelationIdFilter.MDC_KEY),
                        details);
        return ResponseEntity.status(status).body(response);
    }
}
