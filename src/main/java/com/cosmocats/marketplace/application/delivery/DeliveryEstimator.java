package com.cosmocats.marketplace.application.delivery;

import com.cosmocats.marketplace.application.exception.DeliveryServiceException;
import com.cosmocats.marketplace.application.exception.DeliveryTimeoutException;

/**
 * Port: calculates a delivery estimate. The application layer only knows this interface;
 * the 3rd-party HTTP client lives in the infrastructure layer.
 */
public interface DeliveryEstimator {

    /**
     * @throws DeliveryTimeoutException if the provider did not answer in time
     * @throws DeliveryServiceException if the provider failed or is unreachable
     */
    DeliveryEstimate estimate(String destination);
}
