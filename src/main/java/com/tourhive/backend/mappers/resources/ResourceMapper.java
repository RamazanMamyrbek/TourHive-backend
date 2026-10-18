package com.tourhive.backend.mappers.resources;

import com.tourhive.backend.dto.resources.response.ResourceResponseDto;
import com.tourhive.backend.entities.resources.Resource;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ResourceMapper {

    @Mapping(target = "url", expression = "java(\"/api/v1/resources/\" + resource.getId())")
    ResourceResponseDto toResponseDto(Resource resource);
}
