package com.innowise.orderservice.service.impl;

import com.innowise.orderservice.config.JwtEmailExtractor;
import com.innowise.orderservice.exception.ResourceNotFoundException;
import com.innowise.orderservice.mapper.OrderMapper;
import com.innowise.orderservice.model.OrderStatus;
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
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
        existing.setStatus(updatedDto.status());
        existing.getItems().clear();

        List<OrderItem> updatedItems = updatedDto.items().stream().map(dtoItem -> {
            OrderItem item = orderMapper.map(dtoItem);
            item.setOrder(existing);
            Long itemId = item.getItem().getId();
            Item itemEntity = itemRepository.findById(itemId).orElseThrow(() -> new ResourceNotFoundException("Item not found: " + itemId));
            item.setItem(itemEntity);
            return item;
        }).toList();

        existing.getItems().addAll(updatedItems);

        Order saved = orderRepository.save(existing);
        UserDto user = fetchUserById(saved.getUserId());
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


    private Order findOrderById(Long id) {
        return orderRepository.findByIdWithItems(id).
                orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
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
        return Optional.ofNullable(userClient.getUserByEmail(email).block())
                .orElseThrow(() -> new ResourceNotFoundException("User not found by email: " + email));
    }

    private UserDto fetchUserById(Long userId) {
        return Optional.ofNullable(userClient.getUserById(userId).block())
                .orElseThrow(() -> new ResourceNotFoundException("User not found by ID: " + userId));
    }

    private Map<Long, UserDto> fetchUsersByIds(List<Long> userIds) {
        return Optional.ofNullable(
                userClient.getUsersByIds(userIds)
                        .flatMap(page -> Flux.fromIterable(page.getContent()))
                        .collectMap(UserDto::getId)
                        .block()
        ).orElseGet(Collections::emptyMap);
    }

    private OrderDto enrichWithUser(OrderDto dto, UserDto user) {
        return new OrderDto(dto.id(), dto.status(), dto.creationDate(), dto.items(), user);
    }

}
