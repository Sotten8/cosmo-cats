package com.cosmocats.marketplace.web.mapper;

import com.cosmocats.marketplace.application.delivery.DeliveryEstimate;
import com.cosmocats.marketplace.web.dto.response.DeliveryEstimateResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface DeliveryEstimateMapper {

    @Mapping(target = "cost", source = "cost.amount")
    @Mapping(target = "currency", source = "cost.currency")
    DeliveryEstimateResponse toDto(DeliveryEstimate estimate);
}
