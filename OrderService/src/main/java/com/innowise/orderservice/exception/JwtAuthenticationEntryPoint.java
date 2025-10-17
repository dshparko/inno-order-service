package com.innowise.orderservice.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.innowise.orderservice.model.dto.error.ErrorResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

import static com.innowise.orderservice.config.AuthConstant.CONTENT_TYPE;

/**
 * @ClassName JwtAuthenticationEntryPoint
 * @Description Handles unauthorized access attempts by returning 401 error.
 * @Author dshparko
 * @Date 08.10.2025
 */
@RequiredArgsConstructor
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        ErrorResponseDto errorResponse = new ErrorResponseDto(
                HttpServletResponse.SC_UNAUTHORIZED,
                "Unauthorized: " + authException.getMessage(),
                request.getRequestURI(),
                UUID.randomUUID()
        );

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(CONTENT_TYPE);
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}