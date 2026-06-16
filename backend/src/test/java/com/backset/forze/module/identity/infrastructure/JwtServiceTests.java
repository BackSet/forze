package com.backset.forze.module.identity.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import com.backset.forze.configuration.JwtProperties;
import org.junit.jupiter.api.Test;

class JwtServiceTests {

	private final JwtService jwtService = new JwtService(
			new JwtProperties("forze-backend", "12345678901234567890123456789012", Duration.ofMinutes(15), Duration.ofDays(30)),
			Clock.fixed(Instant.parse("2026-06-16T18:00:00Z"), ZoneOffset.UTC));

	@Test
	void issuesAndParsesAccessTokenSubject() {
		UUID userId = UUID.randomUUID();

		String token = jwtService.issueAccessToken(new UserPrincipal(userId, "admin", "hash", true));

		assertThat(jwtService.parseSubject(token)).isEqualTo(userId);
	}
}
