package com.cosmocats.marketplace.infrastructure.delivery;

import com.cosmocats.marketplace.application.delivery.DeliveryEstimate;
import com.cosmocats.marketplace.application.delivery.DeliveryEstimator;
import com.cosmocats.marketplace.application.exception.DeliveryServiceException;
import com.cosmocats.marketplace.application.exception.DeliveryTimeoutException;
import com.cosmocats.marketplace.domain.model.Money;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.http.HttpClient;
import java.net.http.HttpTimeoutException;
import java.util.Locale;
import java.util.concurrent.TimeoutException;

/** Adapter of the {@link DeliveryEstimator} port: calls a 3rd-party delivery service (WireMock locally). */
@Component
public class DeliveryClient implements DeliveryEstimator {

    private final RestClient restClient;

    public DeliveryClient(RestClient.Builder builder, DeliveryClientProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(properties.connectTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());
        this.restClient = builder
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory) // timeouts are set on this client only, not globally
                .build();
    }

    @Override
    public DeliveryEstimate estimate(String destination) {
        DeliveryServiceResponse body;
        try {
            body = restClient.get()
                    .uri(uri -> uri.path("/delivery/estimate")
                            .queryParam("destination", "{destination}")
                            .build(destination))
                    .retrieve()
                    .body(DeliveryServiceResponse.class);
        } catch (RestClientException ex) {
            // covers 4xx/5xx responses, timeouts and connection errors
            if (isTimeout(ex)) {
                throw new DeliveryTimeoutException("Delivery service timed out", ex);
            }
            throw new DeliveryServiceException("Delivery service call failed: " + ex.getMessage(), ex);
        }
        if (body == null) {
            throw new DeliveryServiceException("Delivery service returned an empty response", null);
        }
        return toEstimate(body);
    }

    private static DeliveryEstimate toEstimate(DeliveryServiceResponse body) {
        try {
            return new DeliveryEstimate(body.destination(), body.etaDays(),
                    new Money(body.cost(), body.currency()));
        } catch (RuntimeException ex) { // e.g. missing cost or malformed currency from the provider
            throw new DeliveryServiceException("Delivery service returned an invalid response", ex);
        }
    }

    /** Different HTTP clients report a timeout differently, so the whole cause chain is inspected. */
    private static boolean isTimeout(Throwable error) {
        for (Throwable t = error; t != null; t = t.getCause()) {
            if (t instanceof SocketTimeoutException
                    || t instanceof HttpTimeoutException
                    || t instanceof TimeoutException) {
                return true;
            }
            if (t instanceof IOException && t.getMessage() != null
                    && t.getMessage().toLowerCase(Locale.ROOT).contains("timed out")) {
                return true;
            }
        }
        return false;
    }
}
