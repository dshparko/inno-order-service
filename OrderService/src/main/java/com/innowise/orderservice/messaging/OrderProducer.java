package com.innowise.orderservice.messaging;

import com.innowise.orderservice.model.dto.OrderEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
public class OrderProducer {
    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;
    private final String topicCreateOrder;

    public OrderProducer(
            KafkaTemplate<String, OrderEvent> kafkaTemplate,
            @Value("${spring.kafka.topics.create-order}") String topicCreateOrder) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicCreateOrder = topicCreateOrder;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendCreateOrder(OrderEvent event) {
        if (!isValidEvent(event)) {
            log.warn("Invalid OrderEvent: {}", event);
            return;
        }

        String key = buildKey(event);
        sendToKafka(key, event);
    }

    private boolean isValidEvent(OrderEvent event) {
        return event != null && event.getOrderId() != null && event.getUserId() != null;
    }

    private String buildKey(OrderEvent event) {
        return event.getUserId().toString();
    }

    private void sendToKafka(String key, OrderEvent event) {
        kafkaTemplate.send(topicCreateOrder, key, event)
                .thenAccept(result -> log.info("Sent to Kafka: {}", result))
                .exceptionally(ex -> {
                    log.error("Kafka send failed: {}", event, ex);
                    return null;
                });
    }
}
