package com.transitpulse.auth.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "transitpulse.security.cors")
public record CorsProperties(List<String> allowedOrigins) {
}
