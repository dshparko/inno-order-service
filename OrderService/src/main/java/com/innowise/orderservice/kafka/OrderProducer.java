package com.innowise.orderservice.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.innowise.orderservice.model.dto.OrderEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrderProducer {

    private static final Logger logger = LoggerFactory.getLogger(OrderProducer.class);
    private static final String TOPIC_CREATE_ORDER = "create_order";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public OrderProducer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void sendCreateOrder(OrderEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(TOPIC_CREATE_ORDER, payload);
            logger.info("Sent CREATE_ORDER: {}", payload);
        } catch (JsonProcessingException e) {
            String context = String.format("Failed to serialize PaymentEvent for orderId=%s, userId=%s, amount=%s",
                    event.getOrderId(), event.getUserId(), event.getAmount());
            throw new IllegalStateException(context, e);
        }
    }
}
