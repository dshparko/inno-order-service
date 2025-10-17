package com.innowise.orderservice.controller;


import com.innowise.orderservice.model.dto.OrderDto;
import com.innowise.orderservice.model.dto.OrderFilterDto;
import com.innowise.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * @ClassName OrderController
 * @Description REST controller for managing orders.
 * Provides endpoints for creating, retrieving, updating, deleting, and searching orders.
 * @Author dshparko
 * @Date 13.10.2025 17:14
 * @Version 1.0
 */

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    @PreAuthorize(value = "hasRole('USER') or hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<OrderDto> createOrder(@RequestBody OrderDto order) {
        OrderDto created = orderService.createOrder(order);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    @PreAuthorize(value = "hasRole('USER') or hasRole('ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<OrderDto> getOrderById(@PathVariable Long id) {
        OrderDto order = orderService.getOrderById(id);
        return ResponseEntity.ok(order);
    }
    @PreAuthorize(value = "hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<Page<OrderDto>> search(OrderFilterDto filter,
                                                 @PageableDefault Pageable pageable) {
        Page<OrderDto> page = orderService.searchOrders(filter, pageable);

        return page.hasContent()
                ? ResponseEntity.ok(page)
                : ResponseEntity.noContent().build();
    }
    @PreAuthorize(value = "hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<OrderDto> updateOrder(@PathVariable Long id, @RequestBody OrderDto updated) {
        OrderDto order = orderService.updateOrder(id, updated);
        return ResponseEntity.ok(order);
    }
    @PreAuthorize(value = "hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }
}
