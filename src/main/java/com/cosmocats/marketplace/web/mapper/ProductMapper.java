package com.cosmocats.marketplace.web.mapper;

import com.cosmocats.marketplace.domain.common.PageResult;
import com.cosmocats.marketplace.domain.model.Money;
import com.cosmocats.marketplace.domain.model.Product;
import com.cosmocats.marketplace.web.dto.request.CreateProductRequest;
import com.cosmocats.marketplace.web.dto.request.UpdateProductRequest;
import com.cosmocats.marketplace.web.dto.response.PageResponse;
import com.cosmocats.marketplace.web.dto.response.ProductResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.time.Instant;
import java.util.UUID;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        imports = {UUID.class, Instant.class, Money.class})
public interface ProductMapper {

    @Mapping(target = "price", source = "price.amount")
    @Mapping(target = "currency", source = "price.currency")
    ProductResponse toDto(Product product);

    @Mapping(target = "id", expression = "java(UUID.randomUUID())")
    @Mapping(target = "name", expression = "java(request.name().trim())")
    @Mapping(target = "price", expression = "java(new Money(request.price(), request.currency()))")
    @Mapping(target = "createdAt", expression = "java(Instant.now())")
    Product toDomain(CreateProductRequest request);

    @Mapping(target = "id", expression = "java(UUID.randomUUID())")
    @Mapping(target = "name", expression = "java(request.name().trim())")
    @Mapping(target = "price", expression = "java(new Money(request.price(), request.currency()))")
    @Mapping(target = "createdAt", expression = "java(Instant.now())")
    Product toDomain(UpdateProductRequest request);

    default PageResponse<ProductResponse> toPageResponse(PageResult<Product> page) {
        return new PageResponse<>(
                page.items().stream().map(this::toDto).toList(),
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages());
    }
}
