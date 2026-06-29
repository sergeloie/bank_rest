package com.example.bankcards.mapper;

import com.example.bankcards.dto.person.PersonCreateRequest;
import com.example.bankcards.dto.person.PersonResponse;
import com.example.bankcards.entity.Person;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface PersonMapper {
    Person toEntity(PersonCreateRequest personCreateRequest);

//    PersonCreateRequest toPersonCreateRequest(Person person);

//    Person toEntity(PersonResponse personResponse);

    PersonResponse toPersonResponse(Person person);
}