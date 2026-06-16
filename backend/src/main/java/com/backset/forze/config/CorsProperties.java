package com.backset.forze.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "forze.security.cors")
public record CorsProperties(List<String> allowedOrigins) {
}
