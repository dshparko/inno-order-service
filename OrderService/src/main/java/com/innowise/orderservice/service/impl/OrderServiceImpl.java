package com.innowise.orderservice.service.impl;

import com.innowise.orderservice.config.JwtEmailExtractor;
import com.innowise.orderservice.exception.ResourceNotFoundException;
import com.innowise.orderservice.mapper.OrderMapper;
import com.innowise.orderservice.model.OrderStatus;
import com.innowise.orderservice.model.dto.CreateOrderItemDto;
import com.innowise.orderservice.model.dto.OrderDto;
import com.innowise.orderservice.model.dto.OrderFilterDto;
import com.innowise.orderservice.model.dto.userservice.UserDto;
import com.innowise.orderservice.model.entity.Item;
import com.innowise.orderservice.model.entity.Order;
import com.innowise.orderservice.model.entity.OrderItem;
import com.innowise.orderservice.repository.ItemRepository;
import com.innowise.orderservice.repository.OrderRepository;
import com.innowise.orderservice.service.OrderService;
import com.innowise.orderservice.service.UserClient;
import com.innowise.orderservice.specification.OrderSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @ClassName OrderServiceImpl
 * @Description Service implementation for managing orders and enriching them with user data.
 * @Author dshparko
 * @Date 13.10.2025 17:07
 * @Version 1.0
 */
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ItemRepository itemRepository;
    private final OrderMapper orderMapper;
    private final UserClient userClient;
    private final JwtEmailExtractor jwtEmailExtractor;

    @Transactional
    public OrderDto createOrder(OrderDto createDto) {
        Order order = orderMapper.map(createDto);
        order.setCreationDate(LocalDate.now());
        order.setStatus(OrderStatus.NEW);

        UserDto user = fetchUserByEmail();
        order.setUserId(user.getId());

        enrichItems(order.getItems(), order);
        Order saved = orderRepository.save(order);

        return enrichWithUser(orderMapper.map(saved), user);
    }

    @Transactional(readOnly = true)
    public OrderDto getOrderById(Long id) {
        Order order = findOrderById(id);
        UserDto user = fetchUserByEmail();
        return enrichWithUser(orderMapper.map(order), user);
    }

    @Transactional
    public OrderDto updateOrder(Long id, OrderDto updatedDto) {
        Order existing = findOrderById(id);

        validateStatusTransition(existing.getStatus(), updatedDto.status());
        existing.setStatus(updatedDto.status());

        List<OrderItem> mergedItems = mergeOrderItems(existing, updatedDto.items());
        existing.setItems(mergedItems);

        Order saved = orderRepository.save(existing);
        UserDto user = fetchUserByEmail();

        return enrichWithUser(orderMapper.map(saved), user);
    }

    @Transactional
    public void deleteOrder(Long id) {
        Order order = findOrderById(id);
        orderRepository.delete(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderDto> searchOrders(OrderFilterDto filter, Pageable pageable) {
        Page<Order> orders = orderRepository.findAll(OrderSpecification.from(filter), pageable);
        List<Long> userIds = orders.stream()
                .map(Order::getUserId)
                .distinct()
                .toList();

        Map<Long, UserDto> userMap = fetchUsersByIds(userIds);

        return orders.map(order -> {
            OrderDto dto = orderMapper.map(order);
            UserDto user = userMap.get(order.getUserId());
            return enrichWithUser(dto, user);
        });
    }

    private List<OrderItem> mergeOrderItems(Order existing, List<CreateOrderItemDto> incomingDtos) {
        Map<Long, OrderItem> existingItemsByItemId = existing.getItems().stream()
                .collect(Collectors.toMap(item -> item.getItem().getId(), Function.identity()));

        return incomingDtos.stream().map(dtoItem -> {
            Long itemId = dtoItem.getItemId();
            Item itemEntity = itemRepository.findById(itemId)
                    .orElseThrow(() -> new ResourceNotFoundException("Item not found: " + itemId));

            OrderItem existingItem = existingItemsByItemId.get(itemId);
            if (existingItem != null) {
                existingItem.setQuantity(dtoItem.getQuantity());
                return existingItem;
            } else {
                OrderItem newItem = orderMapper.map(dtoItem);
                newItem.setOrder(existing);
                newItem.setItem(itemEntity);
                return newItem;
            }
        }).toList();
    }

    private Order findOrderById(Long id) {
        return orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
    }

    private void validateStatusTransition(OrderStatus current, OrderStatus target) {
        if (!current.canTransitionTo(target)) {
            throw new IllegalStateException("Invalid status transition: " + current + " -> " + target);
        }
    }

    private void enrichItems(List<OrderItem> items, Order order) {
        items.forEach(orderItem -> {
            orderItem.setOrder(order);
            Long itemId = orderItem.getItem().getId();
            Item item = itemRepository.findById(itemId)
                    .orElseThrow(() -> new ResourceNotFoundException("Item not found: " + itemId));
            orderItem.setItem(item);
        });
    }

    private UserDto fetchUserByEmail() {
        String email = jwtEmailExtractor.extractEmail();
        return Optional.ofNullable(userClient.getUserByEmail(email))
                .orElseThrow(() -> new ResourceNotFoundException("User not found by email: " + email));
    }

    private Map<Long, UserDto> fetchUsersByIds(List<Long> userIds) {
        List<UserDto> users = userClient.getUsersByIds(userIds);
        return users.stream().collect(Collectors.toMap(UserDto::getId, Function.identity()));
    }

    private OrderDto enrichWithUser(OrderDto dto, UserDto user) {
        return new OrderDto(dto.id(), dto.status(), dto.creationDate(), dto.items(), user);
    }
}
