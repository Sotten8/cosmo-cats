package com.cosmocats.marketplace.web;

import com.cosmocats.marketplace.domain.Product;
import com.cosmocats.marketplace.web.dto.ProductRequest;
import com.cosmocats.marketplace.web.dto.ProductResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/**
 * MapStruct generates the implementation at compile time (see build/generated/sources/annotationProcessor).
 * The previous hand-written version is kept in docs/manual-mapper/.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ProductMapper {

    ProductResponse toDto(Product product);

    @Mapping(target = "id", expression = "java(java.util.UUID.randomUUID())")
    @Mapping(target = "createdAt", expression = "java(java.time.Instant.now())")
    Product toDomain(ProductRequest request);
}
