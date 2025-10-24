package com.innowise.orderservice.model.dto.userservice;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * @ClassName UpdateCardRequest
 * @Description DTO used for accepting card update input from API clients.
 * Encapsulates card ID, number, holder name, expiration date, and associated user ID.
 * @Author dshparko
 * @Date 08.09.2025 21:30
 * @Version 1.0
 */
@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class CardDto implements Serializable {

    @NotNull(message = "Card ID is required")
    private Long id;

    @NotBlank(message = "Card number is required")
    @Pattern(regexp = "^\\d{13,19}$", message = "The number should be between 13 and 19 digits")
    private String number;

    @NotBlank(message = "Holder name is required")
    private String holder;

    @Future(message = "Expiration date must be in the future")
    private LocalDate expirationDate;

    @NotNull(message = "User ID is required")
    private Long userId;

}
