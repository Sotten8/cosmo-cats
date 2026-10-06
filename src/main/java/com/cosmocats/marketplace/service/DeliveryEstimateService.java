package com.cosmocats.marketplace.service;

import com.cosmocats.marketplace.integration.delivery.DeliveryClient;
import com.cosmocats.marketplace.integration.delivery.DeliveryEstimate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class DeliveryEstimateService {

    private final ProductService productService;
    private final DeliveryClient deliveryClient;

    public DeliveryEstimateService(ProductService productService, DeliveryClient deliveryClient) {
        this.productService = productService;
        this.deliveryClient = deliveryClient;
    }

    public DeliveryEstimate estimate(UUID productId, String destination) {
        productService.findById(productId); // 404 if the product does not exist
        return deliveryClient.estimate(destination);
    }
}
