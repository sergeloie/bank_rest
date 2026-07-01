package com.example.bankcards.mapper;

import com.example.bankcards.dto.cardblockrequest.CardBlockRequestAdminResponse;
import com.example.bankcards.dto.cardblockrequest.CardBlockRequestRequest;
import com.example.bankcards.dto.cardblockrequest.CardBlockRequestResponse;
import com.example.bankcards.entity.CardBlockRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface CardBlockRequestMapper {

    @Mapping(source = "cardId", target = "card.id")
    @Mapping(source = "personId", target = "person.id")
    CardBlockRequest toEntity(CardBlockRequestRequest request);

    CardBlockRequestResponse toResponse(CardBlockRequest entity);

    @Mapping(source = "card.id", target = "cardId")
    @Mapping(source = "person.id", target = "personId")
    CardBlockRequestAdminResponse toAdminResponse(CardBlockRequest entity);
}
