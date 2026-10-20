package com.tourhive.backend.mappers.notifications;

import com.tourhive.backend.dto.notifications.response.NotificationResponseDto;
import com.tourhive.backend.entities.notifications.Notification;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    @org.mapstruct.Mapping(target = "isRead", source = "read")
    NotificationResponseDto toDto(Notification notification);
}
