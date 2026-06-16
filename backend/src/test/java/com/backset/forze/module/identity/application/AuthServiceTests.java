package com.backset.forze.module.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import com.backset.forze.configuration.JwtProperties;
import com.backset.forze.module.identity.domain.RefreshToken;
import com.backset.forze.module.identity.domain.UserAccount;
import com.backset.forze.module.identity.infrastructure.JwtService;
import com.backset.forze.module.identity.infrastructure.RefreshTokenGenerator;
import com.backset.forze.module.identity.infrastructure.RefreshTokenRepository;
import com.backset.forze.module.identity.infrastructure.TokenHashing;
import com.backset.forze.module.identity.infrastructure.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;

@ExtendWith(MockitoExtension.class)
class AuthServiceTests {

	@Mock
	private AuthenticationManager authenticationManager;

	@Mock
	private UserAccountRepository users;

	@Mock
	private RefreshTokenRepository refreshTokens;

	private final Clock clock = Clock.fixed(Instant.parse("2026-06-16T18:00:00Z"), ZoneOffset.UTC);
	private final TokenHashing tokenHashing = new TokenHashing();
	private AuthService authService;

	@BeforeEach
	void setUp() {
		JwtProperties jwtProperties = new JwtProperties("forze-backend", "12345678901234567890123456789012", Duration.ofMinutes(15), Duration.ofDays(30));
		authService = new AuthService(
				authenticationManager,
				users,
				refreshTokens,
				new JwtService(jwtProperties, clock),
				new FixedRefreshTokenGenerator(),
				tokenHashing,
				jwtProperties,
				clock);
		when(refreshTokens.save(any(RefreshToken.class))).thenAnswer((invocation) -> invocation.getArgument(0));
	}

	@Test
	void loginIssuesAccessAndRefreshTokens() {
		UserAccount user = new UserAccount(UUID.randomUUID(), "admin", null, "hash", true);
		when(users.findByUsername("admin")).thenReturn(Optional.of(user));

		AuthTokens tokens = authService.login("admin", "secret");

		assertThat(tokens.accessToken()).isNotBlank();
		assertThat(tokens.refreshToken()).isEqualTo("refresh-token-1");
		verify(refreshTokens).save(any(RefreshToken.class));
	}

	@Test
	void refreshRotatesRefreshTokenAndRevokesPreviousToken() {
		UserAccount user = new UserAccount(UUID.randomUUID(), "admin", null, "hash", true);
		RefreshToken current = new RefreshToken(
				UUID.randomUUID(),
				user,
				tokenHashing.sha256("current-refresh"),
				UUID.randomUUID(),
				clock.instant(),
				clock.instant().plus(Duration.ofDays(30)));
		when(refreshTokens.findByTokenHash(tokenHashing.sha256("current-refresh"))).thenReturn(Optional.of(current));

		AuthTokens tokens = authService.refresh("current-refresh");

		assertThat(tokens.refreshToken()).isEqualTo("refresh-token-1");
		assertThat(current.activeAt(clock.instant())).isFalse();
	}

	private static final class FixedRefreshTokenGenerator extends RefreshTokenGenerator {

		@Override
		public String generate() {
			return "refresh-token-1";
		}
	}
}
