package com.innowise.orderservice.model.dto;

import com.innowise.orderservice.model.OrderStatus;

import java.util.List;

/**
 * @ClassName OrderFilterDto
 * @Description Data Transfer Object (DTO) for filtering {@link com.innowise.orderservice.model.entity.Order} entities.
 * @Author dshparko
 * @Date 16.10.2025 18:17
 * @Version 1.0
 */
public record OrderFilterDto(
        List<OrderStatus> statuses,
        List<Long> ids
) {
}
