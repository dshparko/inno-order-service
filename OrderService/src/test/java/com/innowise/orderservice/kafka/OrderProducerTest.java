package com.innowise.orderservice.kafka;

import com.innowise.orderservice.messaging.OrderProducer;
import com.innowise.orderservice.model.dto.OrderEvent;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.await;


@SpringBootTest
@EnableKafka
class OrderProducerTest {

    private static final String TOPIC = "create_order";

    private static final KafkaContainer KAFKA =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @DynamicPropertySource
    static void overrideKafkaProps(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("spring.kafka.topics.create-payment", () -> "create-payment-test");
    }

    @BeforeAll
    static void startKafka() {
        KAFKA.start();

        Map<String, Object> config = Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers()
        );

        try (AdminClient adminClient = AdminClient.create(config)) {
            NewTopic topic = new NewTopic(TOPIC, 1, (short) 1);
            adminClient.createTopics(List.of(topic)).all().get();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Kafka topic", e);
        }
    }

    @AfterAll
    static void stopKafka() {
        KAFKA.stop();
    }

    @Autowired
    private KafkaTemplate<String, OrderEvent> kafkaTemplate;


    private KafkaConsumer<String, OrderEvent> consumer;

    @BeforeEach
    void setupConsumer() {
        Map<String, Object> props = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "test-consumer",
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class,
                JsonDeserializer.VALUE_DEFAULT_TYPE, "com.innowise.orderservice.model.dto.OrderEvent",
                JsonDeserializer.TRUSTED_PACKAGES, "com.innowise.orderservice.model.dto"
        );
        consumer = new KafkaConsumer<>(props);
        consumer.subscribe(List.of(TOPIC));
    }

    @AfterEach
    void tearDown() {
        if (consumer != null) consumer.close();
    }

    @Test
    void shouldSendCreateOrderEventToKafka() {
        // given
        OrderEvent event = new OrderEvent();
        event.setOrderId(123L);
        event.setUserId(456L);
        event.setAmount(BigDecimal.valueOf(99.99));

        OrderProducer producer = new OrderProducer(kafkaTemplate, "create_order");
        // when
        producer.sendCreateOrder(event);

        // then
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            ConsumerRecords<String, OrderEvent> records = consumer.poll(Duration.ofMillis(500));
            assertThat(records.isEmpty()).isFalse();

            OrderEvent payload = records.iterator().next().value();

            assertThat(payload.getOrderId()).isEqualTo(123L);
            assertThat(payload.getUserId()).isEqualTo(456L);
            assertThat(payload.getAmount()).isEqualByComparingTo("99.99");
        });
    }

}
