package com.urbanclean.exception;

import com.urbanclean.dto.response.ErrorResponse;
import com.urbanclean.exception.custom.AuthenticationException;
import com.urbanclean.exception.custom.InvalidStateTransitionException;
import com.urbanclean.exception.custom.ResourceNotFoundException;
import com.urbanclean.exception.custom.ValidationException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for REST controllers
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handle authentication exceptions (401 Unauthorized)
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException ex,
            HttpServletRequest request) {
        
        log.warn("Authentication error: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .errorCode("AUTHENTICATION_ERROR")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .path(request.getRequestURI())
                .build();
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Handle access denied exceptions (403 Forbidden)
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex,
            HttpServletRequest request) {
        
        log.warn("Access denied: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .errorCode("ACCESS_DENIED")
                .message("You do not have permission to access this resource")
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.FORBIDDEN.value())
                .path(request.getRequestURI())
                .build();
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    /**
     * Handle validation exceptions (400 Bad Request)
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            ValidationException ex,
            HttpServletRequest request) {
        
        log.warn("Validation error: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .errorCode("VALIDATION_ERROR")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .path(request.getRequestURI())
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle method argument validation exceptions (400 Bad Request)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        
        log.warn("Method argument validation error: {}", ex.getMessage());
        
        Map<String, Object> details = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            details.put(fieldName, errorMessage);
        });
        
        ErrorResponse error = ErrorResponse.builder()
                .errorCode("VALIDATION_ERROR")
                .message("Validation failed for one or more fields")
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .path(request.getRequestURI())
                .details(details)
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle illegal state exceptions (400 Bad Request)
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(
            IllegalStateException ex,
            HttpServletRequest request) {
        
        log.warn("Illegal state: {}", ex.getMessage());
        
        // Determine specific error code based on message
        String errorCode = "ILLEGAL_STATE";
        if (ex.getMessage().contains("feedback already provided")) {
            errorCode = "FEEDBACK_ALREADY_PROVIDED";
        } else if (ex.getMessage().contains("RESUELTO state")) {
            errorCode = "INVALID_TASK_STATE";
        } else if (ex.getMessage().contains("deadline")) {
            errorCode = "FEEDBACK_DEADLINE_PASSED";
        }
        
        ErrorResponse error = ErrorResponse.builder()
                .errorCode(errorCode)
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .path(request.getRequestURI())
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle security exceptions (403 Forbidden)
     */
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ErrorResponse> handleSecurityException(
            SecurityException ex,
            HttpServletRequest request) {
        
        log.warn("Security violation: {}", ex.getMessage());
        
        String errorCode = "SECURITY_VIOLATION";
        if (ex.getMessage().contains("original reporter")) {
            errorCode = "FEEDBACK_UNAUTHORIZED";
        }
        
        ErrorResponse error = ErrorResponse.builder()
                .errorCode(errorCode)
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.FORBIDDEN.value())
                .path(request.getRequestURI())
                .build();
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    /**
     * Handle illegal argument exceptions (400 Bad Request)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex,
            HttpServletRequest request) {
        
        log.warn("Illegal argument: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .errorCode("INVALID_ARGUMENT")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .path(request.getRequestURI())
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle invalid state transition exceptions (400 Bad Request)
     */
    @ExceptionHandler(InvalidStateTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidStateTransitionException(
            InvalidStateTransitionException ex,
            HttpServletRequest request) {
        
        log.warn("Invalid state transition: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .errorCode("INVALID_STATE_TRANSITION")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .path(request.getRequestURI())
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle resource not found exceptions (404 Not Found)
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex,
            HttpServletRequest request) {
        
        log.warn("Resource not found: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .errorCode("RESOURCE_NOT_FOUND")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .path(request.getRequestURI())
                .build();
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Handle generic exceptions (500 Internal Server Error)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request) {
        
        log.error("Internal server error", ex);
        
        ErrorResponse error = ErrorResponse.builder()
                .errorCode("INTERNAL_SERVER_ERROR")
                .message("An unexpected error occurred. Please try again later.")
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .path(request.getRequestURI())
                .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
