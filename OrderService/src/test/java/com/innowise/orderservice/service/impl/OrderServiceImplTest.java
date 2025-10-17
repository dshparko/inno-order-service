package com.innowise.orderservice.service.impl;

import com.innowise.orderservice.config.JwtEmailExtractor;
import com.innowise.orderservice.exception.ResourceNotFoundException;
import com.innowise.orderservice.mapper.OrderMapper;
import com.innowise.orderservice.model.OrderStatus;
import com.innowise.orderservice.model.dto.CreateOrderItemDto;
import com.innowise.orderservice.model.dto.OrderDto;
import com.innowise.orderservice.model.dto.userservice.UserDto;
import com.innowise.orderservice.model.entity.Item;
import com.innowise.orderservice.model.entity.Order;
import com.innowise.orderservice.model.entity.OrderItem;
import com.innowise.orderservice.repository.ItemRepository;
import com.innowise.orderservice.repository.OrderRepository;
import com.innowise.orderservice.service.UserClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

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

    @InjectMocks
    private OrderServiceImpl orderService;

    private OrderDto orderDto;
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
        orderDto = new OrderDto(100L, OrderStatus.NEW, LocalDate.now(), List.of(), user);
        order = new Order();
        order.setId(100L);
        order.setStatus(OrderStatus.NEW);
        order.setCreationDate(LocalDate.now());
        order.setUserId(user.getId());
    }

    @Test
    void createOrder_shouldSaveOrderAndReturnDto() {
        when(jwtEmailExtractor.extractEmail()).thenReturn("test@example.com");
        when(userClient.getUserByEmail("test@example.com")).thenReturn(Mono.just(user));
        when(orderMapper.map(orderDto)).thenReturn(order);
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderMapper.map(order)).thenReturn(orderDto);

        OrderDto result = orderService.createOrder(orderDto);

        assertThat(result).isEqualTo(orderDto);
        verify(orderRepository).save(order);
    }

    @Test
    void getOrderById_shouldReturnOrderDto() {
        when(orderRepository.findByIdWithItems(100L)).thenReturn(Optional.of(order));
        when(jwtEmailExtractor.extractEmail()).thenReturn("test@example.com");
        when(userClient.getUserByEmail("test@example.com")).thenReturn(Mono.just(user));
        when(orderMapper.map(order)).thenReturn(orderDto);

        OrderDto result = orderService.getOrderById(100L);

        assertThat(result).isEqualTo(orderDto);
        verify(orderRepository).findByIdWithItems(100L);
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

    @Test
    void updateOrder_shouldApplyChangesAndReturnDto() {
        Long itemId = 5L;

        CreateOrderItemDto dtoItem = new CreateOrderItemDto(itemId, 2);
        Item entity = new Item();
        entity.setId(itemId);

        OrderItem mappedItem = new OrderItem();
        mappedItem.setItem(entity);

        OrderDto updatedDto = new OrderDto(
                100L,
                OrderStatus.DELIVERED,
                LocalDate.now(),
                List.of(dtoItem),
                user
        );

        when(orderRepository.findByIdWithItems(100L)).thenReturn(Optional.of(order));
        when(orderMapper.map(dtoItem)).thenReturn(mappedItem);
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(entity));
        when(orderRepository.save(order)).thenReturn(order);
        when(userClient.getUserById(user.getId())).thenReturn(Mono.just(user));
        when(orderMapper.map(order)).thenReturn(updatedDto);

        OrderDto result = orderService.updateOrder(100L, updatedDto);

        assertThat(result.status()).isEqualTo(OrderStatus.DELIVERED);
        verify(orderRepository).save(order);
        verify(orderMapper).map(dtoItem);
        verify(itemRepository).findById(itemId);
    }
}
