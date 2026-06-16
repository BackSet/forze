package com.backset.forze.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "forze.bootstrap.admin")
public record BootstrapProperties(
		boolean enabled,
		String username,
		String email,
		String initialPassword
) {
}
