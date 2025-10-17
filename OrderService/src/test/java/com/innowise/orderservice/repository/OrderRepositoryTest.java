package com.innowise.orderservice.repository;

import com.innowise.orderservice.model.OrderStatus;
import com.innowise.orderservice.model.dto.OrderFilterDto;
import com.innowise.orderservice.model.entity.Item;
import com.innowise.orderservice.model.entity.Order;
import com.innowise.orderservice.model.entity.OrderItem;
import com.innowise.orderservice.specification.OrderSpecification;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
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

    private Order order;
    private OrderItem orderItem;

    private Order order1;
    private Order order2;
    private Order order3;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        Item item = Item.builder()
                .name("Laptop")
                .price(BigDecimal.valueOf(1500))
                .build();

        orderItem = OrderItem.builder()
                .item(item)
                .quantity(2)
                .build();

        order = Order.builder()
                .userId(1L)
                .creationDate(LocalDate.now())
                .items(List.of(orderItem))
                .status(OrderStatus.SHIPPED)
                .build();

        orderItem.setOrder(order);

        entityManager.persist(item);
        entityManager.persist(order);
        entityManager.persist(orderItem);
        entityManager.flush();
        entityManager.clear();

        order1 = Order.builder()
                .userId(1L)
                .status(OrderStatus.NEW)
                .creationDate(LocalDate.now())
                .build();

        order2 = Order.builder()
                .userId(2L)
                .status(OrderStatus.PROCESSING)
                .creationDate(LocalDate.now())
                .build();

        order3 = Order.builder()
                .userId(3L)
                .status(OrderStatus.DELIVERED)
                .creationDate(LocalDate.now())
                .build();

        orderRepository.saveAll(List.of(order1, order2, order3));
    }

    @Test
    void testSaveOrder() {

        Order saved = orderRepository.save(order);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUserId()).isEqualTo(1L);
    }

    @Test
    void testFindByIdWithItems() {
        Optional<Order> found = orderRepository.findByIdWithItems(order.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getItems()).isNotEmpty();
        assertThat(found.get().getItems().get(0).getItem().getName()).isEqualTo("Laptop");
    }

    @Test
    void testDeleteById() {
        orderRepository.deleteById(order.getId());
        entityManager.flush();
        entityManager.clear();

        Optional<Order> found = orderRepository.findById(order.getId());
        assertThat(found).isEmpty();
    }


    @Test
    void testFilterByIds() {
        var filterDto = new OrderFilterDto(null, List.of(order1.getId(), order3.getId()));

        Specification<Order> spec = OrderSpecification.from(filterDto);
        List<Order> result = orderRepository.findAll(spec);

        assertThat(result)
                .hasSize(2)
                .extracting(Order::getId)
                .containsExactlyInAnyOrder(order1.getId(), order3.getId());
    }

    @Test
    void testFilterByStatuses() {
        var filterDto = new OrderFilterDto(List.of(OrderStatus.NEW, OrderStatus.DELIVERED), null);

        Specification<Order> spec = OrderSpecification.from(filterDto);
        List<Order> result = orderRepository.findAll(spec);

        assertThat(result)
                .hasSize(2)
                .extracting(Order::getStatus)
                .containsExactlyInAnyOrder(OrderStatus.NEW, OrderStatus.DELIVERED);
    }

    @Test
    void testFilterByIdsAndStatuses() {

        var filterDto = new OrderFilterDto(
                List.of(OrderStatus.PROCESSING, OrderStatus.DELIVERED),
                List.of(order1.getId(), order2.getId())
        );

        Specification<Order> spec = OrderSpecification.from(filterDto);
        List<Order> result = orderRepository.findAll(spec);

        assertThat(result)
                .hasSize(1)
                .contains(order2);
    }

    @Test
    void testFilterEmpty() {
        var filterDto = new OrderFilterDto(null, null);

        Specification<Order> spec = OrderSpecification.from(filterDto);
        List<Order> result = orderRepository.findAll(spec);

        assertThat(result)
                .hasSize(4);
    }

    @Test
    void findAll_withStatusSpecification_shouldReturnFilteredPage() {

        entityManager.persist(order1);
        entityManager.persist(order2);

        Specification<Order> spec = (root, query, cb) ->
                cb.equal(root.get("status"), OrderStatus.NEW);

        var page = orderRepository.findAll(spec, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent().get(0).getStatus()).isEqualTo(OrderStatus.NEW);
    }

    @Test
    void deleteById_shouldRemoveOrder() {
        entityManager.persist(order);

        Long id = order.getId();
        orderRepository.deleteById(id);

        assertThat(orderRepository.findById(id)).isEmpty();
    }

    @Test
    @Transactional
    void findByIdWithItems_shouldReturnOrderWithFetchedItems() {
        Item item = new Item();
        item.setName("Pen");
        item.setPrice(BigDecimal.valueOf(1.99));
        entityManager.persist(item);



        entityManager.persist(order);
        entityManager.flush();
        entityManager.clear();

        Optional<Order> result = orderRepository.findByIdWithItems(order.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getItems()).hasSize(1);
        assertThat(result.get().getItems().get(0).getItem().getName()).isEqualTo("Pen");
    }
}
