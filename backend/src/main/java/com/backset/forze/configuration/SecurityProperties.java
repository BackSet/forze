package com.backset.forze.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "forze.security")
public record SecurityProperties(
		boolean cookieSecure,
		String cookieSameSite,
		String refreshCookieName
) {
}
