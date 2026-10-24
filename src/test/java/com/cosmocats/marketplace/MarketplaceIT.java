package com.cosmocats.marketplace;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MarketplaceIT {

    private static final String PRODUCTS = "/api/v1/products";
    private static final String DELIVERY = "/api/v1/delivery-estimates";
    private static final String TOYS = "11111111-1111-1111-1111-111111111111";
    private static final String FOOD = "22222222-2222-2222-2222-222222222222";

    @RegisterExtension
    static WireMockExtension deliveryStub = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void deliveryProperties(DynamicPropertyRegistry registry) {
        registry.add("cosmo.delivery.base-url", deliveryStub::baseUrl);
        registry.add("cosmo.delivery.read-timeout", () -> "500ms");
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private String body(String name, String categoryId) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "name", name,
                "price", "9.99",
                "currency", "GCR",
                "stock", 5,
                "categoryId", categoryId));
    }

    private String createProduct(String name) throws Exception {
        MvcResult result = mockMvc.perform(post(PRODUCTS).contentType(MediaType.APPLICATION_JSON)
                        .content(body(name, TOYS)))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    // Full CRUD

    @Test
    void productLifecycle() throws Exception {
        String id = createProduct("Integration Comet Toy");

        mockMvc.perform(get(PRODUCTS + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Integration Comet Toy"))
                .andExpect(jsonPath("$.price").value(9.99));

        mockMvc.perform(put(PRODUCTS + "/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content(body("Integration Comet Toy v2", FOOD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("Integration Comet Toy v2"))
                .andExpect(jsonPath("$.categoryId").value(FOOD));

        mockMvc.perform(delete(PRODUCTS + "/" + id)).andExpect(status().isNoContent());
        mockMvc.perform(get(PRODUCTS + "/" + id)).andExpect(status().isNotFound());
        mockMvc.perform(delete(PRODUCTS + "/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void listContainsSeededProductsAndSupportsPaging() throws Exception {
        mockMvc.perform(get(PRODUCTS).param("page", "0").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.totalPages", greaterThanOrEqualTo(2)));
    }

    @Test
    void duplicateNameIsRejectedIgnoringCase() throws Exception {
        mockMvc.perform(post(PRODUCTS).contentType(MediaType.APPLICATION_JSON)
                        .content(body("COSMIC MILK 1L", FOOD)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://cosmo-cats.market/errors/conflict"));
    }

    @Test
    void unknownCategoryIsRejected() throws Exception {
        mockMvc.perform(post(PRODUCTS).contentType(MediaType.APPLICATION_JSON)
                        .content(body("Lost Star Toy", UUID.randomUUID().toString())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("categoryId"))
                .andExpect(jsonPath("$.errors[0].code").value("CategoryNotFound"));
    }

    @Test
    void updatingToAnotherProductsNameIsRejected() throws Exception {
        String id = createProduct("Integration Nebula Ball");

        mockMvc.perform(put(PRODUCTS + "/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content(body("cosmic milk 1l", TOYS)))
                .andExpect(status().isConflict());
    }

    @Test
    void invalidBodyIsRejectedWithProblemDetails() throws Exception {
        mockMvc.perform(post(PRODUCTS).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Fluffy\",\"price\":0,\"currency\":\"GCR\",\"stock\":1,"
                                + "\"categoryId\":\"" + TOYS + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://cosmo-cats.market/errors/validation"))
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.instance").value(PRODUCTS))
                .andExpect(jsonPath("$.errors.length()").value(2));
    }

    @Test
    void unknownRouteReturnsProblemJson() throws Exception {
        mockMvc.perform(get("/api/v1/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("https://cosmo-cats.market/errors/not-found"));
    }

    // Delivery: real DeliveryClient talking to WireMock

    @Test
    void deliveryEstimateIsFetchedFromThirdPartyStub() throws Exception {
        deliveryStub.stubFor(WireMock.get(urlPathEqualTo("/delivery/estimate"))
                .withQueryParam("destination", equalTo("Mars"))
                .willReturn(okJson("""
                        {"destination":"Mars","etaDays":7,"cost":42.50,"currency":"GCR"}""")));

        mockMvc.perform(get(DELIVERY).param("destination", "Mars"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.destination").value("Mars"))
                .andExpect(jsonPath("$.etaDays").value(7))
                .andExpect(jsonPath("$.cost").value(42.5))
                .andExpect(jsonPath("$.currency").value("GCR"));
    }

    @Test
    void upstreamFailureBecomes502() throws Exception {
        deliveryStub.stubFor(WireMock.get(urlPathEqualTo("/delivery/estimate"))
                .withQueryParam("destination", equalTo("Pluto"))
                .willReturn(aResponse().withStatus(503).withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"Pluto courier is on strike\"}")));

        mockMvc.perform(get(DELIVERY).param("destination", "Pluto"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.type").value("https://cosmo-cats.market/errors/bad-gateway"));
    }

    @Test
    void slowUpstreamBecomes504() throws Exception {
        deliveryStub.stubFor(WireMock.get(urlPathEqualTo("/delivery/estimate"))
                .withQueryParam("destination", equalTo("Jupiter"))
                .willReturn(okJson("""
                        {"destination":"Jupiter","etaDays":400,"cost":999.00,"currency":"GCR"}""")
                        .withFixedDelay(1500)));

        mockMvc.perform(get(DELIVERY).param("destination", "Jupiter"))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.type").value("https://cosmo-cats.market/errors/gateway-timeout"));
    }
}
