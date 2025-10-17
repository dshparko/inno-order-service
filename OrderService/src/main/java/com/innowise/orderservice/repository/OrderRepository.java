package com.innowise.orderservice.repository;

import com.innowise.orderservice.model.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * @ClassName OrderRepository
 * @Description Repository interface for accessing and managing {@link Order} entities.
 * @Author dshparko
 * @Date 13.10.2025 16:25
 * @Version 1.0
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    /**
     * Saves the given order entity to the database.
     *
     * @param order the order to persist
     * @return the saved order
     */
    Order save(Order order);

    /**
     * Retrieves an order by its ID, including its associated {@code OrderItem}s and {@code Item}s.
     * Uses {@code LEFT JOIN FETCH} to eagerly load nested relationships.
     *
     * @param id the ID of the order
     * @return an {@code Optional} containing the order with items, if found
     */
    @Query("""
            SELECT o FROM Order o
            LEFT JOIN FETCH o.items i
            LEFT JOIN FETCH i.item
            WHERE o.id = :id
            """)
    Optional<Order> findByIdWithItems(Long id);

    /**
     * Deletes the order with the specified ID.
     *
     * @param id the ID of the order to delete
     */
    void deleteById(Long id);

    /**
     * Retrieves a paginated list of orders matching the given specification.
     *
     * @param from     the specification used for filtering
     * @param pageable pagination and sorting information
     * @return a page of matching orders
     */
    Page<Order> findAll(Specification<Order> from, Pageable pageable);
}
