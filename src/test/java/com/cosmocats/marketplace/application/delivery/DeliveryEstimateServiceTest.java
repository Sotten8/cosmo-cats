package com.cosmocats.marketplace.application.delivery;

import com.cosmocats.marketplace.application.exception.DeliveryServiceException;
import com.cosmocats.marketplace.application.exception.DeliveryTimeoutException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.cosmocats.marketplace.support.TestData.gcr;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryEstimateServiceTest {

    @Mock
    private DeliveryEstimator estimator;
    @InjectMocks
    private DeliveryEstimateService service;

    @Test
    void returnsEstimateFromEstimator() {
        DeliveryEstimate estimate = new DeliveryEstimate("Mars", 7, gcr("42.50"));
        when(estimator.estimate("Mars")).thenReturn(estimate);

        assertThat(service.estimate("Mars")).isEqualTo(estimate);
    }

    @Test
    void propagatesTimeout() {
        when(estimator.estimate("Jupiter")).thenThrow(new DeliveryTimeoutException("timeout", null));

        assertThatThrownBy(() -> service.estimate("Jupiter")).isInstanceOf(DeliveryTimeoutException.class);
    }

    @Test
    void propagatesServiceFailure() {
        when(estimator.estimate("Pluto")).thenThrow(new DeliveryServiceException("down", null));

        assertThatThrownBy(() -> service.estimate("Pluto")).isInstanceOf(DeliveryServiceException.class);
    }
}
