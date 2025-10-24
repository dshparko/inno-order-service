package com.innowise.orderservice.controller;


import com.innowise.orderservice.exception.ApiErrorHandler;
import com.innowise.orderservice.exception.ResourceNotFoundException;
import com.innowise.orderservice.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@WebMvcTest(OrderController.class)
@Import({ApiErrorHandler.class})
@AutoConfigureMockMvc
class OrderControllerExceptionTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @Test
    @WithMockUser(roles = "USER")
    void getOrderById_shouldReturn404_whenNotFound() throws Exception {
        when(orderService.getOrderById(anyLong()))
                .thenThrow(new ResourceNotFoundException("Order not found"));

        mockMvc.perform(get("/api/v1/orders/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Order not found"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void deleteOrder_shouldReturn403_whenUnauthorized() throws Exception {
        mockMvc.perform(delete("/api/v1/orders/5"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturn500_onGenericException() throws Exception {
        when(orderService.getOrderById(anyLong()))
                .thenThrow(new RuntimeException("Unexpected error"));

        mockMvc.perform(get("/api/v1/orders/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Unexpected error"))
                .andExpect(jsonPath("$.path").value("/api/v1/orders/1"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturn400_onValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$[0].field").exists())
                .andExpect(jsonPath("$[0].message").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturn400_onMalformedJson() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"invalid-json\"")
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.path").value("/api/v1/orders"));
    }
}