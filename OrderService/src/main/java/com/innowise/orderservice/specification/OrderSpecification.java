package com.innowise.orderservice.specification;

import com.innowise.orderservice.model.dto.OrderFilterDto;
import com.innowise.orderservice.model.entity.Order;
import jakarta.persistence.criteria.Predicate;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName OrderRepository
 * Utility class for building dynamic {@link Specification} instances for {@link Order} entities
 * based on filtering criteria provided in {@link OrderFilterDto}.
 * @Author dshparko
 * @Date 13.10.2025 16:25
 * @Version 1.0
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class OrderSpecification {

    public static Specification<Order> from(OrderFilterDto filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.ids() != null && !filter.ids().isEmpty()) {
                predicates.add(root.get("id").in(filter.ids()));
            }

            if (filter.statuses() != null && !filter.statuses().isEmpty()) {
                predicates.add(root.get("status").in(filter.statuses()));
            }

            return predicates.isEmpty() ? null : cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
