package com.example.vertexSpace.exception;

import com.example.vertexSpace.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for consistent error responses across all controllers.
 *
 * IMPORTANT:
 * - Do NOT import org.springframework.web.ErrorResponse (name collision with your DTO).
 * - Use com.example.vertexSpace.dto.ErrorResponse (your Lombok @Builder DTO).
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handle validation errors from @Valid annotation.
     */

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();

        ex.getBindingResult().getAllErrors().forEach(err -> {
            if (err instanceof FieldError fe) {
                fieldErrors.put(fe.getField(), fe.getDefaultMessage());
            } else {
                fieldErrors.put(err.getObjectName(), err.getDefaultMessage());
            }
        });

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("Invalid input data")
                .fieldErrors(fieldErrors)
                .build();

        return ResponseEntity.badRequest().body(body);
    }
    @ExceptionHandler(ResourceConflictException.class)
    public ResponseEntity<?> handleResourceConflict(ResourceConflictException ex) {
        log.warn("Resource conflict: {}", ex.getMessage());

        // If conflict details are provided, return them
        if (ex.getConflictDetails() != null) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(ex.getConflictDetails());
        }

        // Otherwise return simple error message
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", Instant.now());
        errorResponse.put("status", HttpStatus.CONFLICT.value());
        errorResponse.put("error", "Resource Conflict");
        errorResponse.put("message", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(errorResponse);
    }

    /**
     * Handle authentication failures (wrong password, user not found).
     */
    @ExceptionHandler({BadCredentialsException.class, UsernameNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleAuthenticationErrors(Exception ex) {
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("Authentication Failed")
                .message("Invalid email or password")
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }
//    @ExceptionHandler(OfferExpiredException.class)
//    public ResponseEntity<ErrorResponse> handleOfferExpired(OfferExpiredException ex) {
//        ErrorResponse error = new ErrorResponse("OFFER_EXPIRED",
//                ex.getMessage(),
//                HttpStatus.GONE.value()
//        );
//        return new ResponseEntity<>(error, HttpStatus.GONE);
//    }

    /**
     * Handle resource not found errors.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Resource Not Found")
                .message(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    /**
     * Handle duplicate resource errors (e.g., email already exists).
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResource(DuplicateResourceException ex) {
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.CONFLICT.value())
                .error("Duplicate Resource")
                .message(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    /**
     * Handle authorization errors (user doesn't have permission).
     */
    @ExceptionHandler(AuthorizationException.class)
    public ResponseEntity<ErrorResponse> handleAuthorizationErrors(AuthorizationException ex) {
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error("Access Denied")
                .message(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    /**
     * Handle general validation errors (business rule violations).
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(ValidationException ex) {
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Error")
                .message(ex.getMessage())
                .build();

        return ResponseEntity.badRequest().body(body);
    }

//    @ExceptionHandler(Exception.class)
//    public ResponseEntity<ErrorResponse> handleGenericErrors(Exception ex) {
//        log.error("Unexpected error occurred", ex);
//
//        ErrorResponse body = ErrorResponse.builder()
//                .timestamp(Instant.now())
//                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
//                .error("Internal Server Error")
//                .message("An unexpected error occurred. Please try again later.")
//                .build();
//
//        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
//    }
}
