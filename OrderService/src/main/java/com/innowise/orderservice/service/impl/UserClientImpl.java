package com.innowise.orderservice.service.impl;

import com.innowise.orderservice.config.JwtTokenProvider;
import com.innowise.orderservice.exception.ResourceNotFoundException;
import com.innowise.orderservice.model.dto.userservice.UserDto;
import com.innowise.orderservice.model.dto.userservice.UserPageDto;
import com.innowise.orderservice.service.UserClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

/**
 * @ClassName UserClient
 * @Description Implementation of {@link UserClient} that communicates with the external User Service
 * @Author dshparko
 * @Date 13.10.2025 21:19
 * @Version 1.0
 */
@Service
public class UserClientImpl implements UserClient {

    private final WebClient webClient;
    private final JwtTokenProvider jwtTokenProvider;
    private final String userApiPath;

    public UserClientImpl(@Value("${user-service.url}") String userServiceUrl,
                          @Value("${user-service.path}") String userApiPath,
                          JwtTokenProvider jwtTokenProvider) {
        this.webClient = WebClient.builder()
                .baseUrl(userServiceUrl)
                .build();
        this.jwtTokenProvider = jwtTokenProvider;
        this.userApiPath = userApiPath;
    }


    public Mono<UserDto> getUserByEmail(String email) {
        String token = jwtTokenProvider.getCurrentToken();

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(userApiPath)
                        .queryParam("email", email)
                        .build())
                .headers(headers -> headers.setBearerAuth(token))
                .retrieve()
                .bodyToMono(UserPageDto.class)
                .flatMap(page -> Optional.ofNullable(page.getContent())
                        .filter(list -> !list.isEmpty())
                        .map(list -> Mono.just(list.get(0)))
                        .orElseGet(() -> Mono.error(new ResourceNotFoundException("User not found"))));
    }

    public Mono<UserDto> getUserById(Long id) {
        return webClient.get()
                .uri(userApiPath + "{id}", id)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response ->
                        response.bodyToMono(String.class)
                                .defaultIfEmpty("User not found")
                                .map(msg -> new ResourceNotFoundException("User not found by ID: " + id + ". Reason: " + msg))
                )
                .bodyToMono(UserDto.class);
    }

    public Flux<UserPageDto> getUsersByIds(List<Long> ids) {
        String token = jwtTokenProvider.getCurrentToken();

        return webClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path(userApiPath);
                    ids.forEach(id -> uriBuilder.queryParam("ids", id));
                    return uriBuilder.build();
                })
                .headers(headers -> headers.setBearerAuth(token))
                .retrieve()
                .bodyToFlux(UserPageDto.class);
    }

}
