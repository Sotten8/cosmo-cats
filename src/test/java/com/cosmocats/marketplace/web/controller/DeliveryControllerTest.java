package com.cosmocats.marketplace.web.controller;

import com.cosmocats.marketplace.application.delivery.DeliveryEstimate;
import com.cosmocats.marketplace.application.delivery.DeliveryEstimateService;
import com.cosmocats.marketplace.application.exception.DeliveryServiceException;
import com.cosmocats.marketplace.application.exception.DeliveryTimeoutException;
import com.cosmocats.marketplace.web.mapper.DeliveryEstimateMapperImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static com.cosmocats.marketplace.support.TestData.gcr;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DeliveryController.class)
@Import(DeliveryEstimateMapperImpl.class)
class DeliveryControllerTest {

    private static final String URL = "/api/v1/delivery-estimates";

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private DeliveryEstimateService service;

    @Test
    void returnsEstimate() throws Exception {
        when(service.estimate("Mars")).thenReturn(new DeliveryEstimate("Mars", 7, gcr("42.50")));

        mockMvc.perform(get(URL).param("destination", "Mars"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.destination").value("Mars"))
                .andExpect(jsonPath("$.etaDays").value(7))
                .andExpect(jsonPath("$.cost").value(42.5))
                .andExpect(jsonPath("$.currency").value("GCR"));
    }

    @Test
    void rejectsMissingDestination() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://cosmo-cats.market/errors/validation"))
                .andExpect(jsonPath("$.errors[0].field").value("destination"))
                .andExpect(jsonPath("$.errors[0].code").value("Required"));
        verifyNoInteractions(service);
    }

    @Test
    void rejectsBlankDestination() throws Exception {
        mockMvc.perform(get(URL).param("destination", "   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("destination"));
        verifyNoInteractions(service);
    }

    @Test
    void rejectsTooLongDestination() throws Exception {
        mockMvc.perform(get(URL).param("destination", "x".repeat(51)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("destination"));
        verifyNoInteractions(service);
    }

    @Test
    void acceptsDestinationOfMaximumLength() throws Exception {
        String destination = "x".repeat(50);
        when(service.estimate(destination)).thenReturn(new DeliveryEstimate(destination, 1, gcr("1.00")));

        mockMvc.perform(get(URL).param("destination", destination)).andExpect(status().isOk());
    }

    @Test
    void returns504WhenDeliveryServiceTimesOut() throws Exception {
        when(service.estimate("Jupiter")).thenThrow(new DeliveryTimeoutException("timeout", null));

        mockMvc.perform(get(URL).param("destination", "Jupiter"))
                .andExpect(status().isGatewayTimeout())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://cosmo-cats.market/errors/gateway-timeout"))
                .andExpect(jsonPath("$.status").value(504));
    }

    @Test
    void returns502WhenDeliveryServiceFails() throws Exception {
        when(service.estimate("Pluto")).thenThrow(new DeliveryServiceException("boom", null));

        mockMvc.perform(get(URL).param("destination", "Pluto"))
                .andExpect(status().isBadGateway())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://cosmo-cats.market/errors/bad-gateway"))
                .andExpect(jsonPath("$.status").value(502));
    }
}
