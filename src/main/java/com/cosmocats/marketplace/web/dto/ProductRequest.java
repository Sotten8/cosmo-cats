package com.cosmocats.marketplace.web.dto;

import com.cosmocats.marketplace.validation.CosmicWordCheck;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductRequest(

        @NotBlank
        @Size(min = 3, max = 100)
        @CosmicWordCheck
        String name,

        @Size(max = 1000)
        String description,

        @NotNull
        @DecimalMin(value = "0.00", inclusive = false, message = "must be greater than 0")
        @Digits(integer = 10, fraction = 2, message = "must have at most 10 integer digits and 2 decimal places")
        BigDecimal price,

        @NotNull
        @PositiveOrZero
        Integer stock,

        @NotNull
        @PositiveOrZero
        Long categoryId,

        @Email
        String sellerEmail
) {
}
