package com.tourhive.backend.mappers.users;

import com.tourhive.backend.dto.auth.response.UserSummaryResponseDto;
import com.tourhive.backend.entities.users.User;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    UserSummaryResponseDto toSummaryDto(User user);
}
