package com.cosmocats.marketplace.web.mapper;

import com.cosmocats.marketplace.domain.common.PageResult;
import com.cosmocats.marketplace.domain.model.Product;
import com.cosmocats.marketplace.web.dto.request.CreateProductRequest;
import com.cosmocats.marketplace.web.dto.request.UpdateProductRequest;
import com.cosmocats.marketplace.web.dto.response.PageResponse;
import com.cosmocats.marketplace.web.dto.response.ProductResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static com.cosmocats.marketplace.support.TestData.TOYS;
import static com.cosmocats.marketplace.support.TestData.gcr;
import static com.cosmocats.marketplace.support.TestData.product;
import static org.assertj.core.api.Assertions.assertThat;

class ProductMapperTest {

    private final ProductMapperImpl mapper = new ProductMapperImpl();

    @Test
    void mapsProductToResponseFlatteningMoney() {
        Product product = product();

        ProductResponse dto = mapper.toDto(product);

        assertThat(dto.id()).isEqualTo(product.id());
        assertThat(dto.name()).isEqualTo(product.name());
        assertThat(dto.description()).isEqualTo(product.description());
        assertThat(dto.price()).isEqualTo(new BigDecimal("19.99"));
        assertThat(dto.currency()).isEqualTo("GCR");
        assertThat(dto.stock()).isEqualTo(product.stock());
        assertThat(dto.categoryId()).isEqualTo(product.categoryId());
        assertThat(dto.createdAt()).isEqualTo(product.createdAt());
    }

    @Test
    void mapsCreateRequest() {
        CreateProductRequest request = new CreateProductRequest("  Star Yarn  ", "desc",
                new BigDecimal("5"), "GCR", 3, TOYS, "a@b.com");
        Instant before = Instant.now();

        Product product = mapper.toDomain(request);

        assertThat(product.id()).isNotNull();
        assertThat(product.name()).isEqualTo("Star Yarn");
        assertThat(product.price()).isEqualTo(gcr("5.00"));
        assertThat(product.stock()).isEqualTo(3);
        assertThat(product.categoryId()).isEqualTo(TOYS);
        assertThat(product.sellerEmail()).isEqualTo("a@b.com");
        assertThat(product.createdAt()).isBetween(before, Instant.now());
    }

    @Test
    void generatesDifferentIdsForEachMapping() {
        CreateProductRequest request = new CreateProductRequest("Star", null,
                BigDecimal.ONE, "GCR", 1, TOYS, null);

        assertThat(mapper.toDomain(request).id()).isNotEqualTo(mapper.toDomain(request).id());
    }

    @Test
    void mapsUpdateRequestToDomain() {
        UpdateProductRequest request = new UpdateProductRequest(" Comet Catnip ", null,
                new BigDecimal("12.00"), "GCR", 0, TOYS, null);

        Product product = mapper.toDomain(request);

        assertThat(product.name()).isEqualTo("Comet Catnip");
        assertThat(product.price()).isEqualTo(gcr("12.00"));
        assertThat(product.stock()).isZero();
        assertThat(product.description()).isNull();
    }

    @Test
    void mapsPageResultToPageResponse() {
        Product product = product();
        PageResult<Product> page = new PageResult<>(List.of(product), 1, 2, 3);

        PageResponse<ProductResponse> response = mapper.toPageResponse(page);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().id()).isEqualTo(product.id());
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.totalElements()).isEqualTo(3);
        assertThat(response.totalPages()).isEqualTo(2);
    }
}