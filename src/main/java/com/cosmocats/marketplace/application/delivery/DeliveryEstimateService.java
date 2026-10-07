package com.cosmocats.marketplace.application.delivery;

import org.springframework.stereotype.Service;

@Service
public class DeliveryEstimateService {

    private final DeliveryEstimator deliveryEstimator;

    public DeliveryEstimateService(DeliveryEstimator deliveryEstimator) {
        this.deliveryEstimator = deliveryEstimator;
    }

    public DeliveryEstimate estimate(String destination) {
        return deliveryEstimator.estimate(destination);
    }
}
