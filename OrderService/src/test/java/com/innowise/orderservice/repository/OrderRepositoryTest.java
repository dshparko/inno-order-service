package com.innowise.orderservice.repository;


import com.innowise.orderservice.model.OrderStatus;
import com.innowise.orderservice.model.entity.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class OrderRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ItemRepository itemRepository;

    @AfterEach
    void cleanDatabase() {
        orderRepository.deleteAll();
        itemRepository.deleteAll();
    }

    @Test
    void shouldSaveAndLoadOrderWithItems() {
        // given
        Item itemA = itemRepository.save(new Item(null, "Item A", BigDecimal.valueOf(10.0)));
        Item itemB = itemRepository.save(new Item(null, "Item B", BigDecimal.valueOf(20.0)));

        Order order = new Order();
        order.setStatus(OrderStatus.NEW);
        order.setCreationDate(LocalDate.now());
        order.setUserId(1L);

        OrderItem oi1 = new OrderItem();
        oi1.setItem(itemA);
        oi1.setQuantity(2);
        oi1.setOrder(order);

        OrderItem oi2 = new OrderItem();
        oi2.setItem(itemB);
        oi2.setQuantity(1);
        oi2.setOrder(order);

        order.setItems(List.of(oi1, oi2));

        // when
        Order saved = orderRepository.save(order);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getItems()).hasSize(2);
        assertThat(saved.getItems().get(0).getItem().getName()).isEqualTo("Item A");

        // when
        Optional<Order> loaded = orderRepository.findByIdWithItems(saved.getId());

        // then
        assertThat(loaded).isPresent();
        assertThat(loaded.get().getItems()).hasSize(2);
        assertThat(loaded.get().getItems().get(1).getItem().getName()).isEqualTo("Item B");
    }

    @Test
    void shouldDeleteOrderById() {
        // given
        Item item = itemRepository.save(new Item(null, "Item C", BigDecimal.valueOf(15.0)));

        Order order = new Order();
        order.setStatus(OrderStatus.NEW);
        order.setCreationDate(LocalDate.now());
        order.setUserId(2L);

        OrderItem orderItem = new OrderItem();
        orderItem.setItem(item);
        orderItem.setQuantity(1);
        orderItem.setOrder(order);

        order.setItems(List.of(orderItem));
        Order saved = orderRepository.save(order);

        // when
        orderRepository.deleteById(saved.getId());

        // then
        Optional<Order> deleted = orderRepository.findById(saved.getId());
        assertThat(deleted).isEmpty();
    }

    @Test
    void shouldFindAllBySpecificationAndPagination() {
        // given
        itemRepository.save(new Item(null, "Item D", BigDecimal.valueOf(30.0)));

        Order order1 = new Order();
        order1.setStatus(OrderStatus.NEW);
        order1.setCreationDate(LocalDate.now());
        order1.setUserId(1L);

        Order order2 = new Order();
        order2.setStatus(OrderStatus.PROCESSING);
        order2.setCreationDate(LocalDate.now());
        order2.setUserId(2L);

        orderRepository.saveAll(List.of(order1, order2));

        // when
        var spec = (Specification<Order>) (root, _, cb) ->
                cb.equal(root.get("status"), OrderStatus.NEW);
        var page = orderRepository.findAll(spec, org.springframework.data.domain.PageRequest.of(0, 10));

        // then
        assertThat(page).isNotNull();
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getStatus()).isEqualTo(OrderStatus.NEW);
    }
}
