package com.transitpulse.report.dto;

import com.transitpulse.report.entity.ReportStatus;
import com.transitpulse.report.entity.ReportType;
import java.time.Instant;

public record ReportResponse(
        Long id,
        ReportAuthorResponse author,
        ReportType type,
        ReportStatus status,
        String lineNumber,
        String stopName,
        String description,
        long confirmationCount,
        int requiredConfirmations,
        Instant createdAt,
        Instant updatedAt,
        Instant verifiedAt,
        Instant expiresAt
) {
}
