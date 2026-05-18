package com.transitpulse.auth.dto;

public record AuthResponse(
        String token,
        AuthUserResponse user
) { }
