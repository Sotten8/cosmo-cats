package com.cosmocats.marketplace.infrastructure.delivery;

import com.cosmocats.marketplace.application.delivery.DeliveryEstimate;
import com.cosmocats.marketplace.application.exception.DeliveryServiceException;
import com.cosmocats.marketplace.application.exception.DeliveryTimeoutException;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeliveryClientWireMockIT {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private DeliveryClient client(Duration readTimeout) {
        return new DeliveryClient(RestClient.builder(),
                new DeliveryClientProperties(wireMock.baseUrl(), Duration.ofSeconds(1), readTimeout));
    }

    private DeliveryClient client() {
        return client(Duration.ofSeconds(2));
    }

    @Test
    void returnsEstimateFromThirdPartyService() {
        wireMock.stubFor(get(urlPathEqualTo("/delivery/estimate"))
                .withQueryParam("destination", equalTo("Mars"))
                .willReturn(okJson("""
                        {"destination":"Mars","etaDays":7,"cost":42.50,"currency":"GCR"}""")));

        DeliveryEstimate estimate = client().estimate("Mars");

        assertThat(estimate.destination()).isEqualTo("Mars");
        assertThat(estimate.etaDays()).isEqualTo(7);
        assertThat(estimate.cost().amount()).isEqualByComparingTo(new BigDecimal("42.50"));
        assertThat(estimate.cost().currency()).isEqualTo("GCR");
    }

    @Test
    void encodesDestinationInQueryString() {
        wireMock.stubFor(get(urlPathEqualTo("/delivery/estimate"))
                .withQueryParam("destination", equalTo("New Earth & Co"))
                .willReturn(okJson("""
                        {"destination":"New Earth & Co","etaDays":3,"cost":10,"currency":"GCR"}""")));

        DeliveryEstimate estimate = client().estimate("New Earth & Co");

        assertThat(estimate.destination()).isEqualTo("New Earth & Co");
        wireMock.verify(1, getRequestedFor(urlPathEqualTo("/delivery/estimate")));
    }

    @Test
    void mapsUpstream503ToDeliveryServiceException() {
        wireMock.stubFor(get(urlPathEqualTo("/delivery/estimate"))
                .willReturn(aResponse().withStatus(503).withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"Pluto courier is on strike\"}")));

        assertThatThrownBy(() -> client().estimate("Pluto"))
                .isInstanceOf(DeliveryServiceException.class)
                .isNotInstanceOf(DeliveryTimeoutException.class);
    }

    @Test
    void mapsUpstream404ToDeliveryServiceException() {
        wireMock.stubFor(get(urlPathEqualTo("/delivery/estimate")).willReturn(aResponse().withStatus(404)));

        assertThatThrownBy(() -> client().estimate("Atlantis"))
                .isInstanceOf(DeliveryServiceException.class)
                .isNotInstanceOf(DeliveryTimeoutException.class);
    }

    @Test
    void mapsSlowUpstreamToDeliveryTimeoutException() {
        wireMock.stubFor(get(urlPathEqualTo("/delivery/estimate"))
                .willReturn(okJson("""
                        {"destination":"Jupiter","etaDays":400,"cost":999.00,"currency":"GCR"}""")
                        .withFixedDelay(1500)));

        assertThatThrownBy(() -> client(Duration.ofMillis(300)).estimate("Jupiter"))
                .isInstanceOf(DeliveryTimeoutException.class);
    }

    @Test
    void mapsEmptyBodyToDeliveryServiceException() {
        wireMock.stubFor(get(urlPathEqualTo("/delivery/estimate")).willReturn(aResponse().withStatus(200)));

        assertThatThrownBy(() -> client().estimate("Moon"))
                .isInstanceOf(DeliveryServiceException.class)
                .hasMessageContaining("empty response");
    }

    @Test
    void mapsMalformedJsonToDeliveryServiceException() {
        wireMock.stubFor(get(urlPathEqualTo("/delivery/estimate"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("this is not json")));

        assertThatThrownBy(() -> client().estimate("Moon"))
                .isInstanceOf(DeliveryServiceException.class);
    }

    @Test
    void mapsInvalidMoneyToDeliveryServiceException() {
        wireMock.stubFor(get(urlPathEqualTo("/delivery/estimate"))
                .willReturn(okJson("""
                        {"destination":"Moon","etaDays":2,"cost":-1,"currency":"GCR"}""")));

        assertThatThrownBy(() -> client().estimate("Moon"))
                .isInstanceOf(DeliveryServiceException.class)
                .hasMessageContaining("invalid response");
    }

    @Test
    void mapsInvalidCurrencyToDeliveryServiceException() {
        wireMock.stubFor(get(urlPathEqualTo("/delivery/estimate"))
                .willReturn(okJson("""
                        {"destination":"Moon","etaDays":2,"cost":9.99,"currency":"credits"}""")));

        assertThatThrownBy(() -> client().estimate("Moon"))
                .isInstanceOf(DeliveryServiceException.class)
                .hasMessageContaining("invalid response");
    }

    @Test
    void mapsConnectionFailureToDeliveryServiceException() {
        DeliveryClient unreachable = new DeliveryClient(RestClient.builder(),
                new DeliveryClientProperties("http://localhost:1", Duration.ofMillis(500), Duration.ofSeconds(1)));

        assertThatThrownBy(() -> unreachable.estimate("Mars"))
                .isInstanceOf(DeliveryServiceException.class);
    }
}
