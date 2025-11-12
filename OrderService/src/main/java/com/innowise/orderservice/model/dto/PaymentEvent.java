package com.innowise.orderservice.model.dto;

import com.innowise.orderservice.model.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PaymentEvent {

    private Long paymentId;
    private Long orderId;
    private PaymentStatus status;

}
