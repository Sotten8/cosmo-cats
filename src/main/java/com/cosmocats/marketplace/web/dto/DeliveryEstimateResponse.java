package com.cosmocats.marketplace.web.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record DeliveryEstimateResponse(
        UUID productId,
        String destination,
        int etaDays,
        BigDecimal cost,
        String currency
) {
}
