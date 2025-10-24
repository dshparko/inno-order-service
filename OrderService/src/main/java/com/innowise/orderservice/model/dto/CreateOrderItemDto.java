package com.innowise.orderservice.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @ClassName CreateOrderItemDto
 * @Description DTO representing a single item in an order.
 * @Author dshparko
 * @Date 16.10.2025 16:11
 * @Version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderItemDto {

    @NotNull(message = "Item ID must not be null")
    private Long itemId;

    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity;
}
