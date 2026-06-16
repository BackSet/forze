package com.backset.forze.module.identity.infrastructure;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import com.backset.forze.configuration.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

	private final JwtProperties properties;
	private final Clock clock;

	public JwtService(JwtProperties properties, Clock clock) {
		this.properties = properties;
		this.clock = clock;
	}

	public String issueAccessToken(UserPrincipal principal) {
		Instant issuedAt = clock.instant();
		Instant expiresAt = issuedAt.plus(properties.accessExpiration());

		return Jwts.builder()
				.issuer(properties.issuer())
				.subject(principal.id().toString())
				.id(UUID.randomUUID().toString())
				.issuedAt(Date.from(issuedAt))
				.expiration(Date.from(expiresAt))
				.claim("username", principal.username())
				.signWith(Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8)))
				.compact();
	}

	public UUID parseSubject(String token) {
		Claims claims = Jwts.parser()
				.verifyWith(Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8)))
				.requireIssuer(properties.issuer())
				.clock(() -> Date.from(clock.instant()))
				.build()
				.parseSignedClaims(token)
				.getPayload();
		return UUID.fromString(claims.getSubject());
	}
}
