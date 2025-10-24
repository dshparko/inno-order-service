package com.innowise.orderservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.innowise.orderservice.model.OrderStatus;
import com.innowise.orderservice.model.dto.CreateOrderItemDto;
import com.innowise.orderservice.model.dto.OrderDto;
import com.innowise.orderservice.model.dto.OrderFilterDto;
import com.innowise.orderservice.model.dto.userservice.UserDto;
import com.innowise.orderservice.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@WebMvcTest(controllers = OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @Autowired
    private ObjectMapper objectMapper;

    private OrderDto sampleOrder;

    @BeforeEach
    void setUp() {
        sampleOrder = new OrderDto(
                1L,
                OrderStatus.NEW,
                LocalDate.now(),
                List.of(new CreateOrderItemDto(10L, 2)),
                new UserDto(5L, "Darya", "Shparko", "darya@example.com",
                        LocalDate.of(2000, 2, 22),
                        Collections.emptyList())
        );
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void createOrder_shouldReturnCreatedOrder() throws Exception {
        Mockito.when(orderService.createOrder(any())).thenReturn(sampleOrder);

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleOrder)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(sampleOrder.id()))
                .andExpect(jsonPath("$.status").value(sampleOrder.status().name()));
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void getOrderById_shouldReturnOrder() throws Exception {
        Mockito.when(orderService.getOrderById(1L)).thenReturn(sampleOrder);

        mockMvc.perform(get("/api/v1/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sampleOrder.id()))
                .andExpect(jsonPath("$.user.name").value("Darya"));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void search_shouldReturnPageOfOrders_whenHasContent() throws Exception {
        Mockito.when(orderService.searchOrders(any(OrderFilterDto.class), any()))
                .thenReturn(new PageImpl<>(List.of(sampleOrder), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/v1/orders")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(sampleOrder.id()));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void updateOrder_shouldReturnUpdatedOrder() throws Exception {
        OrderDto updated = new OrderDto(
                1L,
                OrderStatus.DELIVERED,
                LocalDate.now(),
                List.of(new CreateOrderItemDto(20L, 1)),
                new UserDto(6L, "Alex", "Ivanov", "alex@example.com",
                        LocalDate.of(1995, 5, 15),
                        Collections.emptyList())
        );

        Mockito.when(orderService.updateOrder(eq(1L), any())).thenReturn(updated);

        mockMvc.perform(put("/api/v1/orders/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void deleteOrder_shouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/orders/5"))
                .andExpect(status().isNoContent());

        Mockito.verify(orderService).deleteOrder(5L);
    }

}