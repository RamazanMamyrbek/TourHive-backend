package com.tourhive.backend.mappers.organizers;

import com.tourhive.backend.dto.admin.response.BusinessProfileAdminResponseDto;
import com.tourhive.backend.dto.organizers.response.BusinessDocumentResponseDto;
import com.tourhive.backend.dto.organizers.response.BusinessProfileResponseDto;
import com.tourhive.backend.dto.organizers.response.OrganizerProfileResponseDto;
import com.tourhive.backend.entities.organizers.BusinessDocument;
import com.tourhive.backend.entities.organizers.BusinessProfile;
import com.tourhive.backend.entities.organizers.OrganizerProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OrganizerMapper {

    BusinessDocumentResponseDto toDocumentDto(BusinessDocument document);

    List<BusinessDocumentResponseDto> toDocumentDtoList(List<BusinessDocument> documents);

    @Mapping(target = "documents", source = "documents")
    BusinessProfileResponseDto toBusinessProfileDto(BusinessProfile businessProfile);

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "userEmail", source = "user.email")
    @Mapping(target = "reviewedByEmail", source = "reviewedBy.email")
    @Mapping(target = "documents", source = "documents")
    BusinessProfileAdminResponseDto toAdminResponseDto(BusinessProfile businessProfile);

    @Mapping(target = "id", source = "organizerProfile.id")
    @Mapping(target = "userId", source = "organizerProfile.user.id")
    @Mapping(target = "isOrganizer", source = "organizerProfile.user.organizer")
    @Mapping(target = "isBusinessVerified", source = "organizerProfile.user.businessVerified")
    @Mapping(target = "businessProfile", source = "businessProfileDto")
    OrganizerProfileResponseDto toOrganizerProfileDto(OrganizerProfile organizerProfile, BusinessProfileResponseDto businessProfileDto);
}
