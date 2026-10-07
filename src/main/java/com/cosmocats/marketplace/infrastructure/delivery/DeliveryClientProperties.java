package com.cosmocats.marketplace.infrastructure.delivery;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/** Settings of the 3rd-party delivery service ({@code cosmo.delivery.*}); the timeouts apply only to its client. */
@ConfigurationProperties(prefix = "cosmo.delivery")
public record DeliveryClientProperties(
        String baseUrl,
        @DefaultValue("2s") Duration connectTimeout,
        @DefaultValue("3s") Duration readTimeout
) {
}
