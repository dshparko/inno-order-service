package com.innowise.orderservice.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderEvent {
    @NotNull
    @Min(1)
    private Long orderId;

    @NotNull
    @Min(1)
    private Long userId;

    @NotNull
    @Positive
    private BigDecimal amount;
}