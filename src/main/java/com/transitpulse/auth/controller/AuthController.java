package com.transitpulse.auth.controller;

import com.transitpulse.auth.dto.AuthResponse;
import com.transitpulse.auth.dto.AuthUserResponse;
import com.transitpulse.auth.dto.LoginRequest;
import com.transitpulse.auth.dto.RegisterRequest;
import com.transitpulse.auth.mapper.AuthMapper;
import com.transitpulse.auth.security.AuthenticatedUser;
import com.transitpulse.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthMapper authMapper;

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public AuthUserResponse me(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return authMapper.toResponse(currentUser);
    }
}
