package com.transitpulse.user.dto;

import com.transitpulse.user.entity.Role;
import java.time.Instant;

public record UserResponse(
        Long id,
        String email,
        String displayName,
        Role role,
        Instant createdAt,
        Instant updatedAt
) {
}
