package com.transitpulse.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.transitpulse.auth.dto.AuthResponse;
import com.transitpulse.auth.dto.AuthUserResponse;
import com.transitpulse.auth.dto.LoginRequest;
import com.transitpulse.auth.dto.RegisterRequest;
import com.transitpulse.auth.exception.EmailAlreadyUsedException;
import com.transitpulse.auth.exception.InvalidCredentialsException;
import com.transitpulse.auth.mapper.AuthMapper;
import com.transitpulse.user.entity.Role;
import com.transitpulse.user.entity.User;
import com.transitpulse.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthMapper authMapper;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerNormalizesEmailAndCreatesUserWithUserRole() {
        RegisterRequest request = new RegisterRequest("  TEST@Example.COM  ", "password123", "  Greg  ");
        AuthUserResponse userResponse = new AuthUserResponse(1L, "test@example.com", "Greg", Role.USER);

        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");
        when(authMapper.toResponse(any(User.class))).thenReturn(userResponse);

        AuthResponse response = authService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertEquals("test@example.com", savedUser.getEmail());
        assertEquals("encoded-password", savedUser.getPasswordHash());
        assertEquals("Greg", savedUser.getDisplayName());
        assertEquals(Role.USER, savedUser.getRole());
        assertEquals("jwt-token", response.token());
        assertEquals(userResponse, response.user());
    }

    @Test
    void registerRejectsAlreadyUsedEmail() {
        RegisterRequest request = new RegisterRequest("test@example.com", "password123", "Greg");

        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThrows(EmailAlreadyUsedException.class, () -> authService.register(request));

        verify(userRepository, never()).save(any());
    }

    @Test
    void loginRejectsInvalidPassword() {
        LoginRequest request = new LoginRequest("TEST@example.com", "wrong-password");
        User user = new User("test@example.com", "encoded-password", "Greg", Role.USER);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));

        verify(jwtService, never()).generateToken(any());
    }
}
