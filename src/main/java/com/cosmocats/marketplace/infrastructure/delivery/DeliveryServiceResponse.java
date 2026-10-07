package com.cosmocats.marketplace.infrastructure.delivery;

import java.math.BigDecimal;

/** Wire format of the 3rd-party delivery service. Never leaves the infrastructure layer. */
record DeliveryServiceResponse(String destination, int etaDays, BigDecimal cost, String currency) {
}
