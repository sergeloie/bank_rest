package com.example.bankcards.mapper;

import com.example.bankcards.dto.card.CardCreateRequest;
import com.example.bankcards.dto.card.CardResponse;
import com.example.bankcards.entity.Card;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface CardMapper {
    @Mapping(source = "personId", target = "person.id")
    Card toEntity(CardCreateRequest cardCreateRequest);

    @InheritInverseConfiguration(name = "toEntity")
    CardResponse toCardResponse(Card card);
}
