package com.transitpulse.common.error;

import java.time.Instant;
import java.util.Map;

public record ApiErrorResponse(
        int status,
        String error,
        String message,
        String path,
        Instant timestamp,
        Map<String, String> details
) {
}
