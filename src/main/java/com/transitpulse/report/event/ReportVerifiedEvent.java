package com.transitpulse.report.event;

import com.transitpulse.report.entity.ReportType;
import java.time.Instant;

public record ReportVerifiedEvent(
        Long reportId,
        Long authorId,
        ReportType type,
        String lineNumber,
        String stopName,
        Instant verifiedAt
) {
}
