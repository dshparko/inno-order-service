package com.innowise.orderservice.model.dto;

import com.innowise.orderservice.model.PaymentStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PaymentEvent {
    @NotNull
    @Min(1)
    private String paymentId;

    @NotNull
    @Min(1)
    private Long orderId;

    @NotNull
    private PaymentStatus status;
}