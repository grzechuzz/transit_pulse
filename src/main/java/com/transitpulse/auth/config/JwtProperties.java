package com.transitpulse.auth.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "transitpulse.security.jwt")
public record JwtProperties(String secret, Duration expiration) {
}
