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

/**
 * MapStruct generates the implementation at compile time (see build/generated/sources/annotationProcessor).
 * The previous hand-written version is kept in docs/manual-mapper/ and must behave the same way.
 */
@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        imports = {UUID.class, Instant.class, Money.class})
public interface ProductMapper {

    @Mapping(target = "price", source = "price.amount")
    @Mapping(target = "currency", source = "price.currency")
    ProductResponse toDto(Product product);

    /** Creates a brand-new domain object: generates the id and the creation time, trims the name. */
    @Mapping(target = "id", expression = "java(UUID.randomUUID())")
    @Mapping(target = "name", expression = "java(request.name().trim())")
    @Mapping(target = "price", expression = "java(new Money(request.price(), request.currency()))")
    @Mapping(target = "createdAt", expression = "java(Instant.now())")
    Product toDomain(CreateProductRequest request);

    /** The service keeps the real id and createdAt of the stored product and copies only the editable fields. */
    @Mapping(target = "id", expression = "java(UUID.randomUUID())")
    @Mapping(target = "name", expression = "java(request.name().trim())")
    @Mapping(target = "price", expression = "java(new Money(request.price(), request.currency()))")
    @Mapping(target = "createdAt", expression = "java(Instant.now())")
    Product toDomain(UpdateProductRequest request);

    /** The page metadata (including totalPages) comes from {@link PageResult}; the mapper only converts it. */
    default PageResponse<ProductResponse> toPageResponse(PageResult<Product> page) {
        return new PageResponse<>(
                page.items().stream().map(this::toDto).toList(),
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages());
    }
}
