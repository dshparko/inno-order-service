package com.innowise.orderservice.service.impl;

import com.innowise.orderservice.config.JwtEmailExtractor;
import com.innowise.orderservice.exception.ResourceNotFoundException;
import com.innowise.orderservice.mapper.OrderMapper;
import com.innowise.orderservice.model.OrderStatus;
import com.innowise.orderservice.model.dto.userservice.UserDto;
import com.innowise.orderservice.model.entity.Order;
import com.innowise.orderservice.repository.ItemRepository;
import com.innowise.orderservice.repository.OrderRepository;
import com.innowise.orderservice.service.UserClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ItemRepository itemRepository;
    @Mock
    private OrderMapper orderMapper;
    @Mock
    private UserClient userClient;
    @Mock
    private JwtEmailExtractor jwtEmailExtractor;

    @Spy
    @InjectMocks
    private OrderServiceImpl orderService;

    private Order order;
    private UserDto user;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        user = new UserDto(
                1L,
                "Darya",
                "Shparko",
                "darya@example.com",
                LocalDate.of(1995, 10, 17),
                List.of()
        );

        order = new Order();
        order.setId(100L);
        order.setStatus(OrderStatus.NEW);
        order.setCreationDate(LocalDate.now());
        order.setUserId(user.getId());
    }


    @Test
    void getOrderById_shouldThrowIfNotFound() {
        when(orderRepository.findByIdWithItems(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order not found");
    }

    @Test
    void deleteOrder_shouldRemoveOrder() {
        when(orderRepository.findByIdWithItems(100L)).thenReturn(Optional.of(order));

        orderService.deleteOrder(100L);

        verify(orderRepository).delete(order);
    }

    @Test
    void deleteOrder_shouldThrowIfNotFound() {
        when(orderRepository.findByIdWithItems(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.deleteOrder(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order not found");
    }
}
