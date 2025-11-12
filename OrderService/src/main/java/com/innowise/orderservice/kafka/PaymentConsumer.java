package com.innowise.orderservice.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.innowise.orderservice.model.dto.PaymentEvent;
import com.innowise.orderservice.service.OrderService;
import lombok.AllArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class PaymentConsumer {

    private static final Logger logger = LoggerFactory.getLogger(PaymentConsumer.class);
    private final ObjectMapper objectMapper;
    private final OrderService orderService;


    @KafkaListener(topics = "create_payment", groupId = "order-service-group")
    public void handleCreatePayment(ConsumerRecord<String, String> consumerRecord) {
        try {
            PaymentEvent event = objectMapper.readValue(consumerRecord.value(), PaymentEvent.class);
            logger.info("Received CREATE_PAYMENT: {}", event);

            orderService.updateOrderStatus(event.getOrderId(), event.getStatus());
        } catch (Exception e) {
            logger.error("Failed to deserialize payment event", e);
        }
    }
}
