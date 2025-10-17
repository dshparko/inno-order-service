package com.innowise.orderservice.exception;


import java.io.Serial;
import java.util.UUID;

/**
 * @ClassName UserNotFoundException
 * @Description Custom runtime exception thrown when a  resource is not found.
 * @Author dshparko
 * @Date 11.09.2025 15:50
 * @Version 1.0
 */
public class ResourceNotFoundException extends RuntimeException {
    @Serial
    private final UUID errorId = UUID.randomUUID();

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Exception e) {
        super(message, e);
    }

    public ResourceNotFoundException(Throwable cause) {
        super(cause);
    }
}

