package com.transitpulse.auth.dto;

import com.transitpulse.user.entity.Role;

public record AuthUserResponse(
        Long id,
        String email,
        String displayName,
        Role role
) { }
