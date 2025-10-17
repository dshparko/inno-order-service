package com.innowise.orderservice.model.dto.userservice;

import java.util.List;

/**
 * @ClassName UserPageDto
 * @Description DTO representing a paginated response of users from an external service.
 * @Author dshparko
 * @Date 16.10.2025 14:33
 * @Version 1.0
 */

public class UserPageDto {
    private List<UserDto> content;

    public List<UserDto> getContent() {
        return content;
    }
}
