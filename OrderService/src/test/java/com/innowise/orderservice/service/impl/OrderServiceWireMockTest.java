package com.innowise.orderservice.service.impl;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.innowise.orderservice.OrderServiceApplication;
import com.innowise.orderservice.model.OrderStatus;
import com.innowise.orderservice.model.dto.CreateOrderItemDto;
import com.innowise.orderservice.model.dto.OrderDto;
import com.innowise.orderservice.model.entity.Item;
import com.innowise.orderservice.model.entity.Order;
import com.innowise.orderservice.model.entity.OrderItem;
import com.innowise.orderservice.repository.ItemRepository;
import com.innowise.orderservice.repository.OrderRepository;
import com.innowise.orderservice.service.OrderService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(
        classes = OrderServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
class OrderServiceWireMockTest {

    @RegisterExtension
    static WireMockExtension wiremock = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().dynamicPort())
            .build();

    @Autowired
    private OrderService orderService;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private OrderRepository orderRepository;

    private Long testItemId;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("user-service.url", () -> "http://localhost:" + wiremock.getPort());
        registry.add("user-service.path", () -> "/api/v1/users");
    }

    @BeforeEach
    void setupSecurityContext() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "alice@example.com", "mocked-jwt-token");
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

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

        Item item = itemRepository.save(new Item(null, "Test item", BigDecimal.valueOf(10.0)));
        testItemId = item.getId();
    }

    @AfterEach
    void cleanup() {
        orderRepository.deleteAll();
        itemRepository.deleteAll();
        wiremock.resetAll();
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldCreateOrderWithMockedUser() {
        CreateOrderItemDto itemDto = new CreateOrderItemDto(testItemId, 2);
        OrderDto createDto = new OrderDto(null, OrderStatus.NEW, LocalDate.now(), List.of(itemDto), null);

        OrderDto result = orderService.createOrder(createDto).block();

        assertThat(result).isNotNull();
        assertThat(result.user().getEmail()).isEqualTo("alice@example.com");
        assertThat(result.status()).isEqualTo(OrderStatus.NEW);
    }

    @Test
    void shouldGetOrderByIdWithMockedUser() {
        Item item = itemRepository.save(new Item(null, "Item A", BigDecimal.valueOf(15.0)));

        OrderItem orderItem = new OrderItem();
        orderItem.setItem(item);
        orderItem.setQuantity(1);

        Order order = new Order();
        order.setStatus(OrderStatus.NEW);
        order.setCreationDate(LocalDate.now());
        order.setUserId(1L);
        order.setItems(List.of(orderItem));
        orderItem.setOrder(order);

        Order saved = orderRepository.save(order);

        OrderDto result = orderService.getOrderById(saved.getId()).block();

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(saved.getId());
        assertThat(result.user().getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    void shouldDeleteOrderSuccessfully() {
        Item item = itemRepository.save(new Item(null, "Item C", BigDecimal.valueOf(25.0)));

        OrderItem orderItem = new OrderItem();
        orderItem.setItem(item);
        orderItem.setQuantity(1);

        Order order = new Order();
        order.setStatus(OrderStatus.NEW);
        order.setCreationDate(LocalDate.now());
        order.setUserId(1L);
        orderItem.setOrder(order);
        order.setItems(List.of(orderItem));

        Order saved = orderRepository.save(order);

        orderService.deleteOrder(saved.getId());

        Optional<Order> deleted = orderRepository.findById(saved.getId());
        assertThat(deleted).isEmpty();
    }
}
