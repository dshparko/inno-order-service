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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplUnitTest {

    @InjectMocks
    private OrderServiceImpl orderService;

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

    private final Long userId = 1L;
    private final Long orderId = 10L;
    private final String email = "test@example.com";

    private final UserDto user = new UserDto(
            userId,
            "Alice",
            "Smith",
            "alice@example.com",
            LocalDate.of(1990, 1, 1),
            List.of()
    );

    @Test
    void createOrder_shouldSaveAndReturnEnrichedOrder() {
        OrderDto inputDto = new OrderDto(null, null, null, List.of(), null);

        Item item = new Item(100L, "Item", BigDecimal.TEN);
        OrderItem orderItem = OrderItem.builder()
                .item(item)
                .quantity(2)
                .build();

        Order mappedOrder = new Order();
        mappedOrder.setItems(List.of(orderItem));

        Order savedOrder = new Order();
        savedOrder.setId(orderId);
        savedOrder.setUserId(userId);
        savedOrder.setItems(mappedOrder.getItems());

        OrderDto mappedSavedDto = new OrderDto(orderId, OrderStatus.NEW, LocalDate.now(), List.of(), null);

        when(orderMapper.map(inputDto)).thenReturn(mappedOrder);
        when(jwtEmailExtractor.extractEmail()).thenReturn(email);
        when(userClient.getUserByEmail(email)).thenReturn(user);
        when(itemRepository.findById(100L)).thenReturn(Optional.of(item));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(orderMapper.map(savedOrder)).thenReturn(mappedSavedDto);

        OrderDto result = orderService.createOrder(inputDto);

        assertThat(result.id()).isEqualTo(orderId);
        assertThat(result.user()).isEqualTo(user);
        verify(orderRepository).save(any(Order.class));
    }


    @Test
    void getOrderById_shouldReturnMappedOrder() {
        Order order = new Order();
        order.setId(orderId);
        order.setUserId(userId);

        OrderDto mappedDto = new OrderDto(orderId, OrderStatus.NEW, LocalDate.now(), List.of(), null);

        when(orderRepository.findByIdWithItems(orderId)).thenReturn(Optional.of(order));
        when(jwtEmailExtractor.extractEmail()).thenReturn(email);
        when(userClient.getUserByEmail(email)).thenReturn(user);
        when(orderMapper.map(order)).thenReturn(mappedDto);

        OrderDto result = orderService.getOrderById(orderId);

        assertThat(result.id()).isEqualTo(orderId);
        assertThat(result.user()).isEqualTo(user);
    }


    @Test
    void updateOrder_shouldMergeItemsAndReturnUpdatedDto() {
        Order existing = new Order();
        existing.setId(orderId);
        existing.setStatus(OrderStatus.NEW);
        existing.setItems(new ArrayList<>());
        existing.setUserId(userId);

        CreateOrderItemDto itemDto = new CreateOrderItemDto(100L, 3);
        OrderDto updatedDto = new OrderDto(orderId, OrderStatus.PROCESSING, LocalDate.now(), List.of(itemDto), null);

        Item item = new Item(100L, "Item", BigDecimal.TEN);
        OrderItem mappedItem = OrderItem.builder()
                .item(item)
                .order(existing)
                .quantity(3)
                .build();

        Order saved = new Order();
        saved.setId(orderId);
        saved.setUserId(userId);

        OrderDto mappedSavedDto = new OrderDto(orderId, OrderStatus.PROCESSING, LocalDate.now(), List.of(), null);

        when(orderRepository.findByIdWithItems(orderId)).thenReturn(Optional.of(existing));
        when(itemRepository.findById(100L)).thenReturn(Optional.of(item));
        when(orderMapper.map(itemDto)).thenReturn(mappedItem);
        when(orderRepository.save(existing)).thenReturn(saved);
        when(jwtEmailExtractor.extractEmail()).thenReturn(email);
        when(userClient.getUserByEmail(email)).thenReturn(user);
        when(orderMapper.map(saved)).thenReturn(mappedSavedDto);

        OrderDto result = orderService.updateOrder(orderId, updatedDto);

        assertThat(result.id()).isEqualTo(orderId);
        assertThat(result.status()).isEqualTo(OrderStatus.PROCESSING);
        assertThat(result.user()).isEqualTo(user);
        verify(orderRepository).save(existing);
    }


    @Test
    void deleteOrder_shouldDeleteIfExists() {
        Order order = new Order();
        order.setId(orderId);

        when(orderRepository.findByIdWithItems(orderId)).thenReturn(Optional.of(order));

        orderService.deleteOrder(orderId);

        verify(orderRepository).delete(order);
    }


    @Test
    void validateStatusTransition_shouldThrowIfInvalid() {
        assertThatThrownBy(() ->
                orderService.updateOrder(orderId, new OrderDto(orderId, OrderStatus.NEW, LocalDate.now(), List.of(), null))
        ).isInstanceOf(ResourceNotFoundException.class);
    }
}