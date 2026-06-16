package com.backset.forze.configuration;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "forze.security.jwt")
public record JwtProperties(
		String issuer,
		String secret,
		Duration accessExpiration,
		Duration refreshExpiration
) {
}
