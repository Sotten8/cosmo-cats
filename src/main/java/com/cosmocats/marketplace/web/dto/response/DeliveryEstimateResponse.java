package com.cosmocats.marketplace.web.dto.response;

import java.math.BigDecimal;

public record DeliveryEstimateResponse(
        String destination,
        int etaDays,
        BigDecimal cost,
        String currency
) {
}
