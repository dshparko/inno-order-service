package com.innowise.orderservice.service.impl;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.innowise.orderservice.config.JwtEmailExtractor;
import com.innowise.orderservice.config.JwtTokenProvider;
import com.innowise.orderservice.exception.ResourceNotFoundException;
import com.innowise.orderservice.kafka.OrderProducer;
import com.innowise.orderservice.model.OrderStatus;
import com.innowise.orderservice.model.PaymentStatus;
import com.innowise.orderservice.model.dto.CreateOrderItemDto;
import com.innowise.orderservice.model.dto.OrderDto;
import com.innowise.orderservice.model.dto.userservice.UserDto;
import com.innowise.orderservice.model.entity.Item;
import com.innowise.orderservice.model.entity.Order;
import com.innowise.orderservice.model.entity.OrderItem;
import com.innowise.orderservice.repository.ItemRepository;
import com.innowise.orderservice.repository.OrderRepository;
import com.innowise.orderservice.service.OrderService;
import com.innowise.orderservice.service.UserClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Testcontainers
@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class OrderServiceWireMockTest {

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @RegisterExtension
    private static final WireMockExtension wiremock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("user-service.url", () -> "http://localhost:" + wiremock.getPort());
        registry.add("user-service.path", () -> "/api/v1/users");
        registry.add("jwt.secret", () -> "test-secret");
    }

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private JwtEmailExtractor jwtEmailExtractor;

    @MockitoBean
    private UserClient userClient;

    @MockitoBean
    private OrderProducer orderProducer;

    @Autowired
    private OrderService orderService;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private OrderRepository orderRepository;

    private Long testItemId;

    @BeforeEach
    void setup() {
        wiremock.stubFor(get(urlPathEqualTo("/api/v1/users"))
                .withQueryParam("email", equalTo("alice@example.com"))
                .willReturn(okJson("""
                        {
                          "content": [
                            {
                              "id": 1,
                              "email": "alice@example.com",
                              "firstName": "Darya",
                              "lastName": "Shparko"
                            }
                          ]
                        }
                        """)));

        when(jwtEmailExtractor.extractEmail()).thenReturn("alice@example.com");
        when(userClient.getUserByEmail("alice@example.com"))
                .thenReturn(new UserDto(1L, "Darya", "Shparko", "alice@example.com", LocalDate.of(2011, 11, 11), List.of()));

        Item item = itemRepository.save(new Item(null, "Test item", BigDecimal.valueOf(10.0)));
        testItemId = item.getId();
    }

    @AfterEach
    void cleanup() {
        orderRepository.deleteAll();
        itemRepository.deleteAll();
        wiremock.resetAll();
    }

    @Test
    void shouldCreateOrderAndCallKafka() {
        CreateOrderItemDto itemDto = new CreateOrderItemDto(testItemId, 2);
        OrderDto dto = new OrderDto(null, OrderStatus.NEW, LocalDate.now(), List.of(itemDto), null);

        OrderDto result = orderService.createOrder(dto);

        assertThat(result).isNotNull();
        assertThat(result.user()).isNotNull();
        assertThat(result.user().getEmail()).isEqualTo("alice@example.com");
        assertThat(result.items()).hasSize(1);
        assertThat(result.status()).isEqualTo(OrderStatus.NEW);

        // Проверка, что Kafka отправлен
        verify(orderProducer, times(1)).sendCreateOrder(any());
    }

    @Test
    void shouldGetOrderById() {
        OrderItem orderItem = new OrderItem();
        orderItem.setItem(itemRepository.findById(testItemId).get());
        orderItem.setQuantity(1);

        Order order = new Order();
        order.setUserId(1L);
        order.setStatus(OrderStatus.NEW);
        order.setCreationDate(LocalDate.now());
        order.setItems(List.of(orderItem));
        orderItem.setOrder(order);

        Order saved = orderRepository.save(order);

        OrderDto result = orderService.getOrderById(saved.getId());

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(saved.getId());
        assertThat(result.user()).isNotNull();
        assertThat(result.user().getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    void shouldDeleteOrder() {
        Order order = new Order();
        order.setUserId(1L);
        order.setStatus(OrderStatus.NEW);
        order.setCreationDate(LocalDate.now());
        order.setItems(List.of());
        Order saved = orderRepository.save(order);

        orderService.deleteOrder(saved.getId());

        Optional<Order> deleted = orderRepository.findById(saved.getId());
        assertThat(deleted).isEmpty();
    }

    @Test
    void shouldUpdateOrderStatusCorrectly() {
        Order order = new Order();
        order.setUserId(1L);
        order.setStatus(OrderStatus.NEW);
        order.setCreationDate(LocalDate.now());
        order.setItems(List.of());
        Order saved = orderRepository.save(order);

        orderService.updateOrderStatus(saved.getId(), PaymentStatus.SUCCESS);

        Order updated = orderRepository.findById(saved.getId()).get();
        assertThat(updated.getStatus()).isEqualTo(OrderStatus.PROCESSING);
    }

    @Test
    void shouldThrowOnInvalidStatusTransition() {
        Order order = new Order();
        order.setUserId(1L);
        order.setStatus(OrderStatus.DELIVERED);
        order.setCreationDate(LocalDate.now());
        order.setItems(List.of());
        Order saved = orderRepository.save(order);

        Long savedId = saved.getId();
        assertThatThrownBy(() -> orderService.updateOrderStatus(savedId, PaymentStatus.SUCCESS))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
