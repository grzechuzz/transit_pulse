package com.transitpulse.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.transitpulse.auth.config.JwtProperties;
import com.transitpulse.auth.security.AuthenticatedUser;
import com.transitpulse.user.entity.Role;
import com.transitpulse.user.entity.User;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "12345678901234567890123456789012";

    @Test
    void generatedTokenContainsUserEmailAndIsValidForSameUser() {
        JwtService jwtService = new JwtService(new JwtProperties(SECRET, Duration.ofMinutes(15)));
        User user = user(1L, "test@example.com", Role.USER);

        String token = jwtService.generateToken(user);

        assertEquals("test@example.com", jwtService.extractEmail(token));
        assertTrue(jwtService.isTokenValid(token, AuthenticatedUser.from(user)));
    }

    @Test
    void tokenIsInvalidForDifferentUser() {
        JwtService jwtService = new JwtService(new JwtProperties(SECRET, Duration.ofMinutes(15)));
        User tokenOwner = user(1L, "owner@example.com", Role.USER);
        User otherUser = user(2L, "other@example.com", Role.USER);

        String token = jwtService.generateToken(tokenOwner);

        assertFalse(jwtService.isTokenValid(token, AuthenticatedUser.from(otherUser)));
    }

    @Test
    void invalidTokenReturnsNullEmailAndIsRejected() {
        JwtService jwtService = new JwtService(new JwtProperties(SECRET, Duration.ofMinutes(15)));
        User user = user(1L, "test@example.com", Role.USER);

        assertNull(jwtService.extractEmail("not-a-token"));
        assertFalse(jwtService.isTokenValid("not-a-token", AuthenticatedUser.from(user)));
    }

    private static User user(Long id, String email, Role role) {
        User user = new User(email, "password-hash", "Greg", role);
        user.setId(id);
        return user;
    }
}
