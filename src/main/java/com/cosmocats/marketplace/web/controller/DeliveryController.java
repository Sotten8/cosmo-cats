package com.cosmocats.marketplace.web.controller;

import com.cosmocats.marketplace.application.delivery.DeliveryEstimateService;
import com.cosmocats.marketplace.web.dto.response.DeliveryEstimateResponse;
import com.cosmocats.marketplace.web.mapper.DeliveryEstimateMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The estimate depends only on the destination, so it is its own resource and not a sub-resource of a product.
 * Calls the 3rd-party delivery service through the application layer.
 */
@RestController
@RequestMapping("/api/v1/delivery-estimates")
public class DeliveryController {

    private final DeliveryEstimateService deliveryEstimateService;
    private final DeliveryEstimateMapper mapper;

    public DeliveryController(DeliveryEstimateService deliveryEstimateService, DeliveryEstimateMapper mapper) {
        this.deliveryEstimateService = deliveryEstimateService;
        this.mapper = mapper;
    }

    @GetMapping
    public DeliveryEstimateResponse estimate(@RequestParam @NotBlank @Size(max = 50) String destination) {
        return mapper.toDto(deliveryEstimateService.estimate(destination));
    }
}
