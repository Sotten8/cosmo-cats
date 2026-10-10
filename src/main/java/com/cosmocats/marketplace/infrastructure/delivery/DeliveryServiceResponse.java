package com.cosmocats.marketplace.infrastructure.delivery;

import java.math.BigDecimal;

record DeliveryServiceResponse(String destination, int etaDays, BigDecimal cost, String currency) {
}
