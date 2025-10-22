package com.innowise.orderservice.service;

import com.innowise.orderservice.model.dto.OrderDto;
import com.innowise.orderservice.model.dto.OrderFilterDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service interface for managing {@link com.innowise.orderservice.model.entity.Order} entities.
 * <p>
 * Provides operations for creating, retrieving, updating, deleting, and searching orders.
 */
public interface OrderService {

    /**
     * Creates a new order based on the provided data.
     *
     * @param createDto the data required to create a new order
     * @return the created {@link OrderDto} with generated ID and details
     */
    Mono<OrderDto> createOrder(OrderDto createDto);

    /**
     * Retrieves an order by its unique identifier.
     *
     * @param id the ID of the order to retrieve
     * @return the corresponding {@link OrderDto} if found
     */
    Mono<OrderDto> getOrderById(Long id);

    /**
     * Updates an existing order with the provided data.
     *
     * @param id the ID of the order to update
     * @param updatedDto the updated order details
     * @return the updated {@link OrderDto}
     */
    Mono<OrderDto> updateOrder(Long id, OrderDto updatedDto);

    /**
     * Deletes an order by its unique identifier.
     *
     * @param id the ID of the order to delete
     */
    void deleteOrder(Long id);

    /**
     * Searches for orders using filtering criteria and pagination.
     *
     * @param filter the filter criteria for searching orders
     * @param pageable pagination and sorting information
     * @return a paginated list of matching {@link OrderDto} results
     */
    Flux<Page<OrderDto>> searchOrders(OrderFilterDto filter, Pageable pageable);
}
