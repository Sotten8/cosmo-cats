package com.cosmocats.marketplace.web;

import com.cosmocats.marketplace.domain.Product;
import com.cosmocats.marketplace.integration.delivery.DeliveryEstimate;
import com.cosmocats.marketplace.service.DeliveryEstimateService;
import com.cosmocats.marketplace.service.PageResult;
import com.cosmocats.marketplace.service.ProductService;
import com.cosmocats.marketplace.web.dto.DeliveryEstimateResponse;
import com.cosmocats.marketplace.web.dto.PageResponse;
import com.cosmocats.marketplace.web.dto.ProductRequest;
import com.cosmocats.marketplace.web.dto.ProductResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@Validated
public class ProductController {

    private final ProductService productService;
    private final DeliveryEstimateService deliveryEstimateService;
    private final ProductMapper mapper;

    public ProductController(ProductService productService,
                             DeliveryEstimateService deliveryEstimateService,
                             ProductMapper mapper) {
        this.productService = productService;
        this.deliveryEstimateService = deliveryEstimateService;
        this.mapper = mapper;
    }

    @GetMapping
    public PageResponse<ProductResponse> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {

        PageResult<Product> result = productService.findAll(page, size);
        List<ProductResponse> content = result.items().stream().map(mapper::toDto).toList();
        int totalPages = (int) Math.ceil((double) result.totalElements() / size);
        return new PageResponse<>(content, page, size, result.totalElements(), totalPages);
    }

    @GetMapping("/{id}")
    public ProductResponse getById(@PathVariable UUID id) {
        return mapper.toDto(productService.findById(id));
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        Product created = productService.create(mapper.toDomain(request));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(mapper.toDto(created));
    }

    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable UUID id, @Valid @RequestBody ProductRequest request) {
        return mapper.toDto(productService.update(id, mapper.toDomain(request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /** Calls the 3rd-party delivery service through RestClient. */
    @GetMapping("/{id}/delivery-estimate")
    public DeliveryEstimateResponse deliveryEstimate(
            @PathVariable UUID id,
            @RequestParam @NotBlank @Size(max = 50) String destination) {

        DeliveryEstimate estimate = deliveryEstimateService.estimate(id, destination);
        return new DeliveryEstimateResponse(id, estimate.destination(), estimate.etaDays(),
                estimate.cost(), estimate.currency());
    }
}
