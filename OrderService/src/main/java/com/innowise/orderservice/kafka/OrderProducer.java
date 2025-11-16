package com.innowise.orderservice.kafka;

import com.innowise.orderservice.model.dto.OrderEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrderProducer {

    private static final String TOPIC_CREATE_ORDER = "create_order";

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    public OrderProducer(KafkaTemplate<String, OrderEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendCreateOrder(OrderEvent event) {

        kafkaTemplate.send(TOPIC_CREATE_ORDER, event);

    }
}
