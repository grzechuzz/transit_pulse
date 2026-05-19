package com.transitpulse.notification.dto;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        Long reportId,
        String title,
        String message,
        boolean read,
        Instant createdAt,
        Instant readAt
) {
}
