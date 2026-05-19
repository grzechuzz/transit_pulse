package com.transitpulse.notification.mapper;

import com.transitpulse.notification.dto.NotificationResponse;
import com.transitpulse.notification.entity.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    @Mapping(target = "reportId", source = "report.id")
    @Mapping(target = "read", expression = "java(notification.getReadAt() != null)")
    NotificationResponse toResponse(Notification notification);
}
