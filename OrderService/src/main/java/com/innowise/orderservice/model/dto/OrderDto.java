package com.innowise.orderservice.model.dto;

import com.innowise.orderservice.model.OrderStatus;
import com.innowise.orderservice.model.dto.userservice.UserDto;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;


/**
 * DTO representing an order with its metadata, items, and associated user.
 * Used for transferring order data between layers and services.
 *
 * @param id           unique identifier of the order
 * @param status       current status of the order (e.g., NEW, COMPLETED)
 * @param creationDate date the order was created
 * @param items        list of items included in the order
 * @param user         user who placed the order
 */
public record OrderDto(
        Long id,
        OrderStatus status,
        LocalDate creationDate,
        @NotNull
        List<CreateOrderItemDto> items,
        UserDto user
) {
}
