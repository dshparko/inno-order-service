package com.innowise.orderservice.repository;

import com.innowise.orderservice.model.OrderStatus;
import com.innowise.orderservice.model.dto.OrderFilterDto;
import com.innowise.orderservice.model.entity.Item;
import com.innowise.orderservice.model.entity.Order;
import com.innowise.orderservice.model.entity.OrderItem;
import com.innowise.orderservice.specification.OrderSpecification;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link OrderRepository}.
 */
@DataJpaTest
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private Order baseOrder;
    private Order order1, order2, order3;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();

        Item item = persist(Item.builder()
                .name("Laptop")
                .price(BigDecimal.valueOf(1500))
                .build());

        OrderItem orderItem = OrderItem.builder()
                .item(item)
                .quantity(2)
                .build();

        baseOrder = Order.builder()
                .userId(1L)
                .creationDate(LocalDate.now())
                .items(List.of(orderItem))
                .status(OrderStatus.SHIPPED)
                .build();

        orderItem.setOrder(baseOrder);

        persist(baseOrder);
        persist(orderItem);

        order1 = persist(Order.builder().userId(1L).status(OrderStatus.NEW).creationDate(LocalDate.now()).build());
        order2 = persist(Order.builder().userId(2L).status(OrderStatus.PROCESSING).creationDate(LocalDate.now()).build());
        order3 = persist(Order.builder().userId(3L).status(OrderStatus.DELIVERED).creationDate(LocalDate.now()).build());

        entityManager.flush();
        entityManager.clear();
    }

    private <T> T persist(T entity) {
        entityManager.persist(entity);
        return entity;
    }

    @Test
    void shouldSaveOrder() {
        Order saved = orderRepository.save(baseOrder);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUserId()).isEqualTo(1L);
    }

    @Test
    void shouldDeleteOrderById() {
        orderRepository.deleteById(baseOrder.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(orderRepository.findById(baseOrder.getId())).isEmpty();
    }

    @Test
    void shouldDeletePersistedOrder() {
        Order order = persist(Order.builder()
                .userId(102L)
                .status(OrderStatus.NEW)
                .creationDate(LocalDate.now())
                .items(List.of())
                .build());

        orderRepository.deleteById(order.getId());
        assertThat(orderRepository.findById(order.getId())).isEmpty();
    }

    @Test
    void shouldFindOrderWithItems() {
        Optional<Order> found = orderRepository.findByIdWithItems(baseOrder.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getItems()).hasSize(1);
        assertThat(found.get().getItems().get(0).getItem().getName()).isEqualTo("Laptop");
    }


    @Test
    void shouldFilterByIds() {
        var filterDto = new OrderFilterDto(null, List.of(order1.getId(), order3.getId()));
        var spec = OrderSpecification.from(filterDto);

        List<Order> result = orderRepository.findAll(spec);

        assertThat(result).hasSize(2)
                .extracting(Order::getId)
                .containsExactlyInAnyOrder(order1.getId(), order3.getId());
    }

    @Test
    void shouldFilterByStatuses() {
        var filterDto = new OrderFilterDto(List.of(OrderStatus.NEW, OrderStatus.DELIVERED), null);
        var spec = OrderSpecification.from(filterDto);

        List<Order> result = orderRepository.findAll(spec);

        assertThat(result).hasSize(2)
                .extracting(Order::getStatus)
                .containsExactlyInAnyOrder(OrderStatus.NEW, OrderStatus.DELIVERED);
    }

    @Test
    void shouldFilterByIdsAndStatuses() {
        var filterDto = new OrderFilterDto(
                List.of(OrderStatus.PROCESSING, OrderStatus.DELIVERED),
                List.of(order1.getId(), order2.getId())
        );
        var spec = OrderSpecification.from(filterDto);

        List<Order> result = orderRepository.findAll(spec);

        assertThat(result).hasSize(1);
    }

    @Test
    void shouldReturnAllWhenFilterEmpty() {
        var spec = OrderSpecification.from(new OrderFilterDto(null, null));
        List<Order> result = orderRepository.findAll(spec);

        assertThat(result).hasSize(4);
    }

    @Test
    void shouldReturnPagedOrdersByStatus() {
        persist(Order.builder().userId(103L).status(OrderStatus.NEW).creationDate(LocalDate.now()).items(List.of()).build());
        persist(Order.builder().userId(104L).status(OrderStatus.DELIVERED).creationDate(LocalDate.now()).items(List.of()).build());

        Specification<Order> spec = (root, query, cb) -> cb.equal(root.get("status"), OrderStatus.NEW);
        var page = orderRepository.findAll(spec, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(2)
                .allMatch(o -> o.getStatus() == OrderStatus.NEW);
    }
}
