package com.example.bankcards.mapper;

import com.example.bankcards.dto.person.PersonCreateRequest;
import com.example.bankcards.dto.person.PersonResponse;
import com.example.bankcards.dto.person.PersonUpdateRequest;
import com.example.bankcards.entity.Person;
import org.mapstruct.*;
import org.springframework.stereotype.Component;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface PersonMapper {
    Person toEntity(PersonCreateRequest personCreateRequest);

    PersonResponse toPersonResponse(Person person);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(PersonUpdateRequest request, @MappingTarget Person person);
}
