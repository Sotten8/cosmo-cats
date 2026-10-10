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
                .requestFactory(requestFactory)
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
        } catch (RuntimeException ex) {
            throw new DeliveryServiceException("Delivery service returned an invalid response", ex);
        }
    }

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
