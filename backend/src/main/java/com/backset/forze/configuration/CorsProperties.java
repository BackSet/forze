package com.backset.forze.configuration;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "forze.security.cors")
public record CorsProperties(List<String> allowedOrigins) {
}
