package com.cosmocats.marketplace.application.delivery;

import com.cosmocats.marketplace.domain.model.Money;

public record DeliveryEstimate(String destination, int etaDays, Money cost) {
}
