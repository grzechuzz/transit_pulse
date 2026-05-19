package com.transitpulse.user.dto;

import com.transitpulse.user.entity.Role;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRoleRequest(
        @NotNull Role role
) {
}
