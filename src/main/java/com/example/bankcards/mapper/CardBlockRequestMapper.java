package com.example.bankcards.mapper;

import com.example.bankcards.dto.cardblockrequest.CardBlockRequestCreate;
import com.example.bankcards.dto.cardblockrequest.CardBlockRequestDto;
import com.example.bankcards.entity.CardBlockRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface CardBlockRequestMapper {

    @Mapping(source = "cardId", target = "card.id")
    @Mapping(source = "personId", target = "person.id")
    CardBlockRequest toEntity(CardBlockRequestCreate create);

    @Mapping(source = "card.id", target = "cardId")
    @Mapping(source = "person.id", target = "personId")
    CardBlockRequestDto toDto(CardBlockRequest entity);
}
