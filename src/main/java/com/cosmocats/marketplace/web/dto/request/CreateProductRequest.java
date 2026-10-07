package com.cosmocats.marketplace.web.dto.request;

import com.cosmocats.marketplace.domain.model.Product;
import com.cosmocats.marketplace.web.validation.CosmicWordCheck;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = false)
public record CreateProductRequest(

        @NotBlank
        @Size(min = 3, max = Product.NAME_MAX_LENGTH)
        @CosmicWordCheck
        String name,

        @Size(max = Product.DESCRIPTION_MAX_LENGTH)
        String description,

        @NotNull
        @DecimalMin(value = "0.00", inclusive = false, message = "must be greater than 0")
        @DecimalMax(value = Product.MAX_PRICE)
        @Digits(integer = 7, fraction = 2, message = "must have at most 7 integer digits and 2 decimal places")
        BigDecimal price,

        @NotNull
        @Pattern(regexp = "[A-Z]{3}", message = "must be a 3-letter uppercase currency code, e.g. GCR")
        String currency,

        @NotNull
        @PositiveOrZero
        @Max(Product.MAX_STOCK)
        Integer stock,

        @NotNull
        UUID categoryId,

        @Email
        String sellerEmail
) {
}
