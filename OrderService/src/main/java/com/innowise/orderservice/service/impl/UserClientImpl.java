package com.innowise.orderservice.service.impl;

import com.innowise.orderservice.config.JwtTokenProvider;
import com.innowise.orderservice.exception.ResourceNotFoundException;
import com.innowise.orderservice.model.dto.userservice.UserDto;
import com.innowise.orderservice.model.dto.userservice.UserPageDto;
import com.innowise.orderservice.service.UserClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.innowise.orderservice.config.AuthConstant.IDS_PARAM;
import static com.innowise.orderservice.config.AuthConstant.KEY_VALUE_SEPARATOR;
import static com.innowise.orderservice.config.AuthConstant.PARAM_SEPARATOR;
import static com.innowise.orderservice.config.AuthConstant.QUERY_PREFIX;
import static com.innowise.orderservice.config.AuthConstant.SLASH;

/**
 * @ClassName UserClient
 * @Description Implementation of {@link UserClient} that communicates with the external User Service
 * @Author dshparko
 * @Date 13.10.2025 21:19
 * @Version 1.0
 */
@Service
public class UserClientImpl implements UserClient {
    private final JwtTokenProvider jwtTokenProvider;
    private final RestTemplate restTemplate;

    @Value("${user-service.url}")
    private String userServiceUrl;

    @Value("${user-service.path}")
    private String userApiPath;

    public UserClientImpl(JwtTokenProvider jwtTokenProvider,
                          @Value("${user-service.url}") String userServiceUrl,
                          @Value("${user-service.path}") String userApiPath,
                          RestTemplate restTemplate) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userServiceUrl = userServiceUrl;
        this.userApiPath = userApiPath;
        this.restTemplate = restTemplate;
    }

    @CircuitBreaker(name = "user-service", fallbackMethod = "fallbackGetUser")
    public UserDto getUserByEmail(String email) {
        String url = buildBaseUrl() + QUERY_PREFIX + "email=" + email;
        ResponseEntity<UserPageDto> response = restTemplate.exchange(url, HttpMethod.GET, buildAuthEntity(), UserPageDto.class);
        return extractFirstUser(response.getBody(), "User not found");
    }

    @CircuitBreaker(name = "user-service", fallbackMethod = "fallbackGetUserById")
    public UserDto getUserById(Long id) {
        String url = buildBaseUrl() + SLASH + id;
        try {
            return restTemplate.getForObject(url, UserDto.class);
        } catch (Exception ex) {
            throw new ResourceNotFoundException("User not found by ID: " + id + ". Reason: " + ex.getMessage());
        }
    }

    @CircuitBreaker(name = "user-service", fallbackMethod = "fallbackGetUsers")
    public List<UserDto> getUsersByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("User ID list must not be empty");
        }

        String url = buildBaseUrl() + buildQueryParam(IDS_PARAM, ids);
        ResponseEntity<UserPageDto> response = restTemplate.exchange(url, HttpMethod.GET, buildAuthEntity(), UserPageDto.class);
        return Optional.ofNullable(response.getBody())
                .map(UserPageDto::getContent)
                .filter(list -> !list.isEmpty())
                .orElseThrow(() -> new ResourceNotFoundException("User list is empty"));
    }

    private HttpEntity<Void> buildAuthEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtTokenProvider.getCurrentToken());
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        return new HttpEntity<>(headers);
    }

    private String buildBaseUrl() {
        String base = userServiceUrl.endsWith(SLASH) ? userServiceUrl.substring(0, userServiceUrl.length() - 1) : userServiceUrl;
        String path = userApiPath.startsWith(SLASH) ? userApiPath : SLASH + userApiPath;
        return base + path;
    }

    private String buildQueryParam(String key, List<Long> values) {
        StringBuilder builder = new StringBuilder(QUERY_PREFIX);
        for (int i = 0; i < values.size(); i++) {
            builder.append(key).append(KEY_VALUE_SEPARATOR).append(values.get(i));
            if (i < values.size() - 1) {
                builder.append(PARAM_SEPARATOR);
            }
        }
        return builder.toString();
    }

    private UserDto extractFirstUser(UserPageDto page, String errorMessage) {
        return Optional.ofNullable(page)
                .map(UserPageDto::getContent)
                .filter(list -> !list.isEmpty())
                .map(list -> list.get(0))
                .orElseThrow(() -> new ResourceNotFoundException(errorMessage));
    }

    public UserDto fallbackGetUser(Throwable throwable) {
        throw new ResourceNotFoundException("User service unavailable. Reason: " + throwable.getMessage());
    }

    public UserDto fallbackGetUserById(Long id, Throwable throwable) {
        throw new ResourceNotFoundException("User service unavailable. Failed to fetch user by ID: " + id +
                ". Reason: " + throwable.getMessage());
    }

    public List<UserDto> fallbackGetUsers(List<Long> ids, Throwable throwable) {
        throw new ResourceNotFoundException("User service is unavailable. Failed to fetch users by IDs: " + ids +
                ". Reason: " + throwable.getMessage());
    }
}
