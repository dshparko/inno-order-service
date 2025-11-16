package com.innowise.orderservice.kafka;

import com.innowise.orderservice.model.dto.PaymentEvent;
import com.innowise.orderservice.service.OrderService;
import lombok.AllArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class PaymentConsumer {

    private final OrderService orderService;

    @KafkaListener(topics = "${spring.kafka.topics.create-payment}", groupId = "order-service-group")
    public void handleCreatePayment(@Payload PaymentEvent event) {
        orderService.updateOrderStatus(event.getOrderId(), event.getStatus());
    }

}
