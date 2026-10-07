package com.cosmocats.marketplace.application.delivery;

import com.cosmocats.marketplace.domain.model.Money;

/** Result of a delivery estimation, expressed in our own terms (not the 3rd-party wire format). */
public record DeliveryEstimate(String destination, int etaDays, Money cost) {
}
