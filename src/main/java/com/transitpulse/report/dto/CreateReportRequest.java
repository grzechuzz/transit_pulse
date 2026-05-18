package com.transitpulse.report.dto;

import com.transitpulse.report.entity.ReportType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record CreateReportRequest(
        @NotNull ReportType type,
        @Size(max = 50) String lineNumber,
        @Size(max = 255) String stopName,
        @NotBlank @Size(max = 2000) String description,
        Instant expiresAt
) {
}
