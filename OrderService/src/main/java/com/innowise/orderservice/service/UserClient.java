package com.innowise.orderservice.service;

import com.innowise.orderservice.model.dto.userservice.UserDto;
import com.innowise.orderservice.model.dto.userservice.UserPageDto;
import reactor.core.publisher.Mono;

import java.util.List;

public interface UserClient {
    /**
     * Retrieves a user by their email address.
     *
     * @param email the email address to search for
     * @return a {@link Mono} emitting the {@link UserDto} if found, or an error if not
     */
    UserDto getUserByEmail(String email);

    /**
     * Retrieves a user by their unique ID.
     *
     * @param id the user ID
     * @return a {@link Mono} emitting the {@link UserDto} if found, or empty if not
     */
    UserDto getUserById(Long id);
    /**
     * Retrieves a users by their unique IDs.
     *
     * @param ids the users IDs
     * @return a {@link Mono} emitting the {@link UserPageDto} if found, or empty if not
     */
    List<UserDto> getUsersByIds(List<Long> ids);

}
