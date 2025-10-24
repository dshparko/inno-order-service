package com.innowise.orderservice.service.impl;

import com.innowise.orderservice.config.JwtTokenProvider;
import com.innowise.orderservice.exception.ResourceNotFoundException;
import com.innowise.orderservice.model.dto.userservice.UserDto;
import com.innowise.orderservice.model.dto.userservice.UserPageDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserClientImplTest {
    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private UserClientImpl userClient;

    private final String baseUrl = "http://localhost:8083";
    private final String apiPath = "/api/v1/users";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        userClient = new UserClientImpl(jwtTokenProvider, baseUrl, apiPath, restTemplate);

        try {
            var field = UserClientImpl.class.getDeclaredField("restTemplate");
            field.setAccessible(true);
            field.set(userClient, restTemplate);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        when(jwtTokenProvider.getCurrentToken()).thenReturn("mock-jwt-token");
    }

    @Test
    void testGetUserByEmail_Success() {
        String email = "test@example.com";
        UserDto user = new UserDto();
        user.setId(1L);
        user.setEmail(email);

        UserPageDto page = new UserPageDto();
        page.setContent(List.of(user));

        ResponseEntity<UserPageDto> response = new ResponseEntity<>(page, HttpStatus.OK);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(UserPageDto.class))
        ).thenReturn(response);

        UserDto result = userClient.getUserByEmail(email);

        assertNotNull(result);
        assertEquals(email, result.getEmail());
        verify(restTemplate, times(1)).exchange(anyString(), eq(HttpMethod.GET), any(), eq(UserPageDto.class));
    }

    @Test
    void testGetUserByEmail_NotFound() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(UserPageDto.class)))
                .thenReturn(new ResponseEntity<>(new UserPageDto(), HttpStatus.OK));

        assertThrows(ResourceNotFoundException.class, () -> userClient.getUserByEmail("missing@example.com"));
    }

    @Test
    void testGetUserById_Success() {
        Long userId = 42L;
        UserDto user = new UserDto();
        user.setId(userId);
        user.setEmail("user42@example.com");

        String expectedUrl = baseUrl + apiPath + "/" + userId;

        when(restTemplate.getForObject(expectedUrl, UserDto.class)).thenReturn(user);

        UserDto result = userClient.getUserById(userId);

        assertNotNull(result);
        assertEquals(userId, result.getId());
        assertEquals("user42@example.com", result.getEmail());
        verify(restTemplate, times(1)).getForObject(expectedUrl, UserDto.class);
    }

    @Test
    void testGetUserById_NotFound() {
        Long userId = 999L;
        String expectedUrl = baseUrl + apiPath + "/" + userId;

        when(restTemplate.getForObject(expectedUrl, UserDto.class)).thenThrow(new RuntimeException("404"));

        assertThrows(ResourceNotFoundException.class, () -> userClient.getUserById(userId));
    }

    @Test
    void testGetUsersByIds_Success() {
        List<Long> ids = List.of(1L, 2L);

        UserDto user = new UserDto();
        user.setId(1L);
        user.setEmail("user1@example.com");

        UserPageDto page = new UserPageDto();
        page.setContent(List.of(user));

        ResponseEntity<UserPageDto> response = new ResponseEntity<>(page, HttpStatus.OK);

        String expectedUrl = baseUrl + apiPath + "?ids=1&ids=2";

        when(restTemplate.exchange(eq(expectedUrl), eq(HttpMethod.GET), any(HttpEntity.class), eq(UserPageDto.class)))
                .thenReturn(response);

        List<UserDto> result = userClient.getUsersByIds(ids);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId().longValue());
        verify(restTemplate, times(1)).exchange(eq(expectedUrl), eq(HttpMethod.GET), any(), eq(UserPageDto.class));
    }

    @Test
    void testGetUsersByIds_Empty() {
        List<Long> ids = List.of(1L);

        UserPageDto page = new UserPageDto();
        page.setContent(List.of());

        ResponseEntity<UserPageDto> response = new ResponseEntity<>(page, HttpStatus.OK);

        String expectedUrl = baseUrl + apiPath + "?ids=1";

        when(restTemplate.exchange(eq(expectedUrl), eq(HttpMethod.GET), any(HttpEntity.class), eq(UserPageDto.class)))
                .thenReturn(response);

        assertThrows(ResourceNotFoundException.class, () -> userClient.getUsersByIds(ids));
    }

    @Test
    void testFallbackGetUser() {
        Throwable cause = new RuntimeException("Timeout");
        assertThrows(ResourceNotFoundException.class, () -> userClient.fallbackGetUser(cause));
    }

    @Test
    void testFallbackGetUserById() {
        Throwable cause = new RuntimeException("Service down");
        assertThrows(ResourceNotFoundException.class, () -> userClient.fallbackGetUserById(42L, cause));
    }

    @Test
    void testFallbackGetUsers() {
        Throwable cause = new RuntimeException("Circuit open");
        List temp = List.of(1L, 2L);
        assertThrows(ResourceNotFoundException.class, () -> {
            userClient.fallbackGetUsers(temp, cause);
        });
    }

}
