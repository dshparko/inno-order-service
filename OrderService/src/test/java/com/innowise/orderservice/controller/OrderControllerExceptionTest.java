package com.innowise.orderservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.innowise.orderservice.exception.ApiErrorHandler;
import com.innowise.orderservice.exception.ResourceNotFoundException;
import org.springframework.http.MediaType;
import com.innowise.orderservice.model.dto.OrderDto;
import com.innowise.orderservice.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@Import(ApiErrorHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class OrderControllerExceptionTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldReturn403_whenAccessDenied() throws Exception {
        doThrow(new AccessDeniedException("Forbidden")).when(orderService).deleteOrder(1L);

        mockMvc.perform(delete("/api/v1/orders/1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void shouldReturn404_whenOrderNotFound() throws Exception {
        when(orderService.getOrderById(999L)).thenThrow(new ResourceNotFoundException("Order not found"));

        mockMvc.perform(get("/api/v1/orders/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Order not found"));
    }

    @Test
    void shouldReturn400_whenMalformedJson() throws Exception {
        mockMvc.perform(post("/api/v1/orders"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn500_whenUnhandledExceptionOccurs() throws Exception {

        when(orderService.getOrderById(1L)).thenThrow(new RuntimeException("Unexpected failure"));

        mockMvc.perform(get("/api/v1/orders/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Unexpected failure"))
                .andExpect(jsonPath("$.path").value("/api/v1/orders/1"));
    }

    @Test
    void shouldReturn400_whenValidationFails() throws Exception {

        OrderDto invalidDto = new OrderDto(null,
                null,
                null,
                null,
                null);

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$[0].field").exists())
                .andExpect(jsonPath("$[0].message").exists());
    }
}
