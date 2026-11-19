package com.lifesync.lifesync.exception;

import com.lifesync.lifesync.ai.exception.AiProviderException;
import com.lifesync.lifesync.ai.exception.AiContextException;
import com.lifesync.lifesync.common.ApiConstants;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(e -> errors.putIfAbsent(e.getField(), e.getDefaultMessage()));
        return response(HttpStatus.BAD_REQUEST, ApiConstants.VALIDATION_FAILED, errors);
    }
    @ExceptionHandler({BadRequestException.class, ConstraintViolationException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ApiError> badRequest(Exception ex) { return response(HttpStatus.BAD_REQUEST, safeMessage(ex, ApiConstants.INVALID_REQUEST), null); }
    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<ApiError> invalidRequestParameter(Exception ex) {
        return response(HttpStatus.BAD_REQUEST, "A request parameter is missing or invalid", null);
    }
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiError> unauthorized(Exception ex) { return response(HttpStatus.UNAUTHORIZED, ex.getMessage(), null); }
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiError> duplicate(Exception ex) { return response(HttpStatus.CONFLICT, ex.getMessage(), null); }
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> notFound(Exception ex) { return response(HttpStatus.NOT_FOUND, ex.getMessage(), null); }
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> accessDenied(AccessDeniedException ex) { return response(HttpStatus.FORBIDDEN, ApiConstants.ACCESS_DENIED, null); }
    @ExceptionHandler(AiProviderException.class)
    public ResponseEntity<ApiError> aiProviderUnavailable(AiProviderException ex) {
        HttpStatus status = switch (ex.getFailureType()) {
            case CONFIGURATION, CONNECTION -> HttpStatus.SERVICE_UNAVAILABLE;
            case RATE_LIMIT -> HttpStatus.TOO_MANY_REQUESTS;
            case TIMEOUT -> HttpStatus.GATEWAY_TIMEOUT;
            case UNAVAILABLE -> HttpStatus.BAD_GATEWAY;
        };
        ResponseEntity.BodyBuilder response = ResponseEntity.status(status);
        if (ex.getFailureType() == AiProviderException.FailureType.RATE_LIMIT && ex.getRetryAfter() != null) {
            response.header(HttpHeaders.RETRY_AFTER, ex.getRetryAfter());
        }
        return response.body(new ApiError(status.value(), status.getReasonPhrase(), ex.getMessage(), null));
    }
    @ExceptionHandler(AiContextException.class)
    public ResponseEntity<ApiError> aiContextUnavailable(AiContextException ex) {
        return response(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), null);
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> integrity() { return response(HttpStatus.CONFLICT, ApiConstants.DATA_CONFLICT, null); }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> internal() { return response(HttpStatus.INTERNAL_SERVER_ERROR, ApiConstants.UNEXPECTED_ERROR, null); }

    private String safeMessage(Exception ex, String fallback) {
        String message = ex.getMessage();
        return message == null || message.isBlank() ? fallback : message;
    }
    private ResponseEntity<ApiError> response(HttpStatus status, String message, Map<String, String> fields) {
        return ResponseEntity.status(status).body(new ApiError(status.value(), status.getReasonPhrase(), message, fields));
    }
}
