package com.cosmocats.marketplace.web.mapper;

import com.cosmocats.marketplace.application.delivery.DeliveryEstimate;
import com.cosmocats.marketplace.web.dto.response.DeliveryEstimateResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.cosmocats.marketplace.support.TestData.gcr;
import static org.assertj.core.api.Assertions.assertThat;

class DeliveryEstimateMapperTest {

    private final DeliveryEstimateMapperImpl mapper = new DeliveryEstimateMapperImpl();

    @Test
    void mapsDeliveryEstimate() {
        DeliveryEstimateResponse dto = mapper.toDto(new DeliveryEstimate("Mars", 7, gcr("42.50")));

        assertThat(dto.destination()).isEqualTo("Mars");
        assertThat(dto.etaDays()).isEqualTo(7);
        assertThat(dto.cost()).isEqualTo(new BigDecimal("42.50"));
        assertThat(dto.currency()).isEqualTo("GCR");
    }
}