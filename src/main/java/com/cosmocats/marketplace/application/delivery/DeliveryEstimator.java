package com.cosmocats.marketplace.application.delivery;

import com.cosmocats.marketplace.application.exception.DeliveryServiceException;
import com.cosmocats.marketplace.application.exception.DeliveryTimeoutException;


public interface DeliveryEstimator {

    DeliveryEstimate estimate(String destination);
}
