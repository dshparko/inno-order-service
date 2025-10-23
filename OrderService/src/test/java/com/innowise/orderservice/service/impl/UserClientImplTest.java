package com.innowise.orderservice.service.impl;

import com.innowise.orderservice.config.JwtTokenProvider;
import com.innowise.orderservice.exception.ResourceNotFoundException;
import com.innowise.orderservice.model.dto.userservice.UserDto;
import com.innowise.orderservice.model.dto.userservice.UserPageDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.Mockito.*;

@ActiveProfiles("test")
class UserClientImplTest {

    private JwtTokenProvider jwtTokenProvider;
    private UserClientImpl userClient;

    private final String userApiPath = "/api/users/";

    @BeforeEach
    void setUp() {
        jwtTokenProvider = mock(JwtTokenProvider.class);
    }

    @Test
    void getUserByEmail_shouldReturnUser() {
        String json = """
                {
                  "content": [
                    {
                      "id": 1,
                      "name": "Darya",
                      "surname": "Shparko",
                      "email": "darya@example.com",
                      "birthDate": "1995-10-17",
                      "cards": []
                    }
                  ]
                }
                """;

        ExchangeFunction exchangeFunction = request ->
                Mono.just(ClientResponse.create(HttpStatus.OK)
                        .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .body(json)
                        .build());

        WebClient mockWebClient = WebClient.builder()
                .exchangeFunction(exchangeFunction)
                .build();

        userClient = new UserClientImpl(mockWebClient, userApiPath, jwtTokenProvider);
        when(jwtTokenProvider.getCurrentToken()).thenReturn("mock-token");

        Mono<UserDto> result = userClient.getUserByEmail("darya@example.com");

        StepVerifier.create(result)
                .expectNextMatches(user -> user.getEmail().equals("darya@example.com"))
                .verifyComplete();
    }

    @Test
    void getUserByEmail_shouldThrowIfEmpty() {
        String emptyJson = """
                {
                  "content": []
                }
                """;

        ExchangeFunction exchangeFunction = request ->
                Mono.just(ClientResponse.create(HttpStatus.OK)
                        .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .body(emptyJson)
                        .build());

        WebClient mockWebClient = WebClient.builder()
                .exchangeFunction(exchangeFunction)
                .build();

        userClient = new UserClientImpl(mockWebClient, userApiPath, jwtTokenProvider);
        when(jwtTokenProvider.getCurrentToken()).thenReturn("mock-token");

        Mono<UserDto> result = userClient.getUserByEmail("notfound@example.com");

        StepVerifier.create(result)
                .expectError(ResourceNotFoundException.class)
                .verify();
    }

    @Test
    void getUserById_shouldReturnUser() {
        String json = """
                {
                  "id": 1,
                  "name": "Darya",
                  "surname": "Shparko",
                  "email": "darya@example.com",
                  "birthDate": "1995-10-17",
                  "cards": []
                }
                """;

        ExchangeFunction exchangeFunction = request ->
                Mono.just(ClientResponse.create(HttpStatus.OK)
                        .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .body(json)
                        .build());

        WebClient mockWebClient = WebClient.builder()
                .exchangeFunction(exchangeFunction)
                .build();

        userClient = new UserClientImpl(mockWebClient, userApiPath, jwtTokenProvider);

        Mono<UserDto> result = userClient.getUserById(1L);

        StepVerifier.create(result)
                .expectNextMatches(user -> user.getId().equals(1L))
                .verifyComplete();
    }

    @Test
    void getUsersByIds_shouldReturnFluxOfPages() {
        String json = """
                {
                  "content": [
                    {
                      "id": 1,
                      "name": "Darya",
                      "surname": "Shparko",
                      "email": "darya@example.com",
                      "birthDate": "1995-10-17",
                      "cards": []
                    }
                  ]
                }
                """;

        ExchangeFunction exchangeFunction = request ->
                Mono.just(ClientResponse.create(HttpStatus.OK)
                        .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .body(json)
                        .build());

        WebClient mockWebClient = WebClient.builder()
                .exchangeFunction(exchangeFunction)
                .build();

        userClient = new UserClientImpl(mockWebClient, userApiPath, jwtTokenProvider);
        when(jwtTokenProvider.getCurrentToken()).thenReturn("mock-token");

        Flux<UserPageDto> result = userClient.getUsersByIds(List.of(1L, 2L));

        StepVerifier.create(result)
                .expectNextMatches(page -> page.getContent() != null && !page.getContent().isEmpty())
                .verifyComplete();
    }
}