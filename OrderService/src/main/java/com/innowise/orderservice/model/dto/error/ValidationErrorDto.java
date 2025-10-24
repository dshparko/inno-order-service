package com.innowise.orderservice.model.dto.error;

/**
 * @ClassName ValidationErrorDto
 * @Description DTO used for exposing field-level validation errors in API responses.
 * Encapsulates the name of the invalid field, the validation message, and the rejected value.
 * @Author dshparko
 * @Date 16.09.2025 15:34
 * @Version 1.0
 */
public record ValidationErrorDto(
        String field,
        String message,
        String rejectedValue
) {
}
