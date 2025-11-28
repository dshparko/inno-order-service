package com.innowise.orderservice.messaging;

import com.innowise.orderservice.model.dto.PaymentEvent;
import com.innowise.orderservice.service.OrderService;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class PaymentConsumer {

    private static final Logger logger = LoggerFactory.getLogger(PaymentConsumer.class);
    private final OrderService orderService;

    @KafkaListener(topics = "${spring.kafka.topics.create-payment}", groupId = "order-service-group")
    public void handleCreatePayment(@Payload PaymentEvent event) {
        if (!isValidEvent(event)) {
            logger.warn("Invalid PaymentEvent: {}", event);
            return;
        }

        try {
            updateOrderStatus(event);
        } catch (Exception ex) {
            handleProcessingError(event, ex);
        }
    }

    private boolean isValidEvent(PaymentEvent event) {
        return event != null && event.getOrderId() != null && event.getStatus() != null;
    }

    private void updateOrderStatus(PaymentEvent event) {
        orderService.updateOrderStatus(event.getOrderId(), event.getStatus());
        logger.info("Updated order {} to status '{}'", event.getOrderId(), event.getStatus());
    }

    private void handleProcessingError(PaymentEvent event, Exception ex) {
        logger.error("Failed to process PaymentEvent: {}", event, ex);
    }
}
