package com.innowise.orderservice.specification;

import com.innowise.orderservice.model.OrderStatus;
import com.innowise.orderservice.model.dto.OrderFilterDto;
import com.innowise.orderservice.model.entity.Order;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderSpecificationTest {

    @Mock
    private Root<Order> root;

    @Mock
    private CriteriaQuery<?> query;

    @Mock
    private CriteriaBuilder cb;

    @Mock
    private Path<Object> idPath;

    @Mock
    private Path<Object> statusPath;

    @Test
    void shouldReturnNullWhenNoFiltersProvided() {
        OrderFilterDto filter = new OrderFilterDto(null, null);
        Specification<Order> spec = OrderSpecification.from(filter);

        Predicate result = spec.toPredicate(root, query, cb);

        assertNull(result);
    }

    @Test
    void shouldFilterByIdsOnly() {
        List<Long> ids = List.of(1L, 2L, 3L);
        OrderFilterDto filter = new OrderFilterDto(null, ids);
        Specification<Order> spec = OrderSpecification.from(filter);

        when(root.get("id")).thenReturn(idPath);
        Predicate idPredicate = mock(Predicate.class);
        when(idPath.in(ids)).thenReturn(idPredicate);
        when(cb.and(idPredicate)).thenReturn(idPredicate);

        Predicate result = spec.toPredicate(root, query, cb);

        assertEquals(idPredicate, result);
    }

    @Test
    void shouldFilterByStatusesOnly() {
        List<OrderStatus> statuses = List.of(OrderStatus.NEW, OrderStatus.CANCELLED);
        OrderFilterDto filter = new OrderFilterDto(statuses, null);
        Specification<Order> spec = OrderSpecification.from(filter);

        when(root.get("status")).thenReturn(statusPath);
        Predicate statusPredicate = mock(Predicate.class);
        when(statusPath.in(statuses)).thenReturn(statusPredicate);
        when(cb.and(statusPredicate)).thenReturn(statusPredicate);

        Predicate result = spec.toPredicate(root, query, cb);

        assertEquals(statusPredicate, result);
    }

}
