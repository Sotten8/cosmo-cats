package com.cosmocats.marketplace.integration.delivery;

import java.math.BigDecimal;

/** Response of the external delivery service. */
public record DeliveryEstimate(String destination, int etaDays, BigDecimal cost, String currency) {
}
