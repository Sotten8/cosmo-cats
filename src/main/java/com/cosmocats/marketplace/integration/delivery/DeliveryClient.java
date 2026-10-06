package com.cosmocats.marketplace.integration.delivery;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Client of a 3rd-party delivery service (stubbed by WireMock locally). */
@Component
public class DeliveryClient {

    private final RestClient restClient;

    public DeliveryClient(RestClient.Builder builder,
                          @Value("${cosmo.delivery.base-url}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public DeliveryEstimate estimate(String destination) {
        try {
            DeliveryEstimate body = restClient.get()
                    .uri(uri -> uri.path("/delivery/estimate")
                            .queryParam("destination", "{destination}")
                            .build(destination))
                    .retrieve()
                    .body(DeliveryEstimate.class);
            if (body == null) {
                throw new DeliveryServiceException("Delivery service returned an empty response", null);
            }
            return body;
        } catch (RestClientException ex) {
            // covers 4xx/5xx responses, timeouts and connection errors
            throw new DeliveryServiceException("Delivery service call failed: " + ex.getMessage(), ex);
        }
    }
}
