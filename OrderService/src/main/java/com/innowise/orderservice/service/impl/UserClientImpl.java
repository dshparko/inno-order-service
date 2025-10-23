package com.innowise.orderservice.service.impl;

import com.innowise.orderservice.config.JwtTokenProvider;
import com.innowise.orderservice.exception.ResourceNotFoundException;
import com.innowise.orderservice.model.dto.userservice.UserDto;
import com.innowise.orderservice.model.dto.userservice.UserPageDto;
import com.innowise.orderservice.service.UserClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

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

    @Value("${user-service.url}")
    private String userServiceUrl;

    @Value("${user-service.path}")
    private String userApiPath;

    public UserClientImpl(JwtTokenProvider jwtTokenProvider,
                          @Value("${user-service.url}")
                          String userServiceUrl,
                          @Value("${user-service.path}")
                          String userApiPath) {
        this.userServiceUrl = userServiceUrl;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userApiPath = userApiPath;

    }

    private final RestTemplate restTemplate = new RestTemplate();

    @CircuitBreaker(name = "user-service", fallbackMethod = "fallbackGetUser")
    public UserDto getUserByEmail(String email) {
        String token = jwtTokenProvider.getCurrentToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        String url = userServiceUrl + userApiPath + "?email=" + email;
        ResponseEntity<UserPageDto> response = restTemplate.exchange(url, HttpMethod.GET, entity, UserPageDto.class);

        UserPageDto page = response.getBody();

        return Optional.ofNullable(page)
                .map(UserPageDto::getContent)
                .filter(list -> !list.isEmpty())
                .map(list -> list.get(0))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @CircuitBreaker(name = "user-service", fallbackMethod = "fallbackGetUserById")
    public UserDto getUserById(Long id) {
        String url = buildAbsoluteUrl(String.valueOf(id));
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

        String token = jwtTokenProvider.getCurrentToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        String url = userServiceUrl + userApiPath + buildQueryParam("ids", ids);

        ResponseEntity<UserPageDto> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                UserPageDto.class
        );

        UserPageDto page = response.getBody();

        return Optional.ofNullable(page)
                .map(UserPageDto::getContent)
                .filter(list -> !list.isEmpty())
                .orElseThrow(() -> new ResourceNotFoundException("User list is empty"));
    }

    private String buildQueryParam(String key, List<Long> values) {
        StringBuilder builder = new StringBuilder("?");
        for (int i = 0; i < values.size(); i++) {
            builder.append(key).append("=").append(values.get(i));
            if (i < values.size() - 1) {
                builder.append("&");
            }
        }
        return builder.toString();
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

    private String buildAbsoluteUrl(String pathSuffix) {
        if (userServiceUrl == null || userApiPath == null) {
            throw new IllegalStateException("User service URL or path is not initialized");
        }

        String base = userServiceUrl.endsWith(SLASH) ? userServiceUrl.substring(0, userServiceUrl.length() - 1) : userServiceUrl;
        String path = userApiPath.startsWith(SLASH) ? userApiPath : SLASH + userApiPath;

        return base + path + (pathSuffix.startsWith(SLASH) ? pathSuffix : SLASH + pathSuffix);
    }

}
