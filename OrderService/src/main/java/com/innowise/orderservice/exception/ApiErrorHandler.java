package com.innowise.orderservice.exception;

import com.innowise.orderservice.model.dto.error.ErrorResponseDto;
import com.innowise.orderservice.model.dto.error.ValidationErrorDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.UUID;

/**
 * @ClassName ApiErrorHandler
 * @Description Global exception handler for REST API.
 * Supports validation errors, resource not found exceptions, malformed requests, and generic server errors.
 * @Author dshparko
 * @Date 11.09.2025 21:05
 * @Version 1.0
 */
@RestControllerAdvice
public class ApiErrorHandler {
    private ErrorResponseDto buildErrorResponse(HttpStatus status, String message, String path) {
        return new ErrorResponseDto(
                status.value(),
                message,
                path,
                UUID.randomUUID()
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDto> handleAccessDenied(AccessDeniedException ex,
                                                               HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(buildErrorResponse(HttpStatus.FORBIDDEN, ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<List<ValidationErrorDto>> handleValidationErrors(MethodArgumentNotValidException ex) {
        List<ValidationErrorDto> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new ValidationErrorDto(
                        error.getField(),
                        error.getDefaultMessage(),
                        error.getRejectedValue() != null ? error.getRejectedValue().toString() : null
                ))
                .toList();

        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleNotFound(ResourceNotFoundException ex,
                                                           HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(HttpMessageConversionException.class)
    public ResponseEntity<ErrorResponseDto> handleBadRequest(HttpMessageConversionException ex,
                                                             HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI()));
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleOther(Exception ex,
                                                        HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), request.getRequestURI()));
    }
}
