package com.backset.forze.module.identity.application;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import com.backset.forze.configuration.JwtProperties;
import com.backset.forze.module.identity.domain.RefreshToken;
import com.backset.forze.module.identity.domain.UserAccount;
import com.backset.forze.module.identity.infrastructure.JwtService;
import com.backset.forze.module.identity.infrastructure.RefreshTokenGenerator;
import com.backset.forze.module.identity.infrastructure.RefreshTokenRepository;
import com.backset.forze.module.identity.infrastructure.TokenHashing;
import com.backset.forze.module.identity.infrastructure.UserAccountRepository;
import com.backset.forze.module.identity.infrastructure.UserPrincipal;
import com.backset.forze.shared.api.ApiException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(prefix = "forze.identity", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AuthService {

	private final AuthenticationManager authenticationManager;
	private final UserAccountRepository users;
	private final RefreshTokenRepository refreshTokens;
	private final JwtService jwtService;
	private final RefreshTokenGenerator refreshTokenGenerator;
	private final TokenHashing tokenHashing;
	private final JwtProperties jwtProperties;
	private final Clock clock;

	public AuthService(
			AuthenticationManager authenticationManager,
			UserAccountRepository users,
			RefreshTokenRepository refreshTokens,
			JwtService jwtService,
			RefreshTokenGenerator refreshTokenGenerator,
			TokenHashing tokenHashing,
			JwtProperties jwtProperties,
			Clock clock
	) {
		this.authenticationManager = authenticationManager;
		this.users = users;
		this.refreshTokens = refreshTokens;
		this.jwtService = jwtService;
		this.refreshTokenGenerator = refreshTokenGenerator;
		this.tokenHashing = tokenHashing;
		this.jwtProperties = jwtProperties;
		this.clock = clock;
	}

	@Transactional
	public AuthTokens login(String username, String password) {
		try {
			authenticationManager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(username, password));
		}
		catch (BadCredentialsException exception) {
			throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials.");
		}

		UserAccount user = users.findByUsername(username)
				.filter(UserAccount::enabled)
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials."));
		return issueSession(user, UUID.randomUUID()).tokens();
	}

	@Transactional
	public AuthTokens refresh(String rawRefreshToken) {
		if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
			throw new ApiException(HttpStatus.UNAUTHORIZED, "Refresh token is required.");
		}

		Instant now = clock.instant();
		RefreshToken current = refreshTokens.findByTokenHash(tokenHashing.sha256(rawRefreshToken))
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid refresh token."));

		if (!current.activeAt(now)) {
			markFamilyReused(current.familyId(), now);
			throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid refresh token.");
		}

		TokenSession replacement = issueSession(current.user(), current.familyId());
		current.revoke(now, replacement.refreshToken().id());
		refreshTokens.save(current);
		return replacement.tokens();
	}

	@Transactional
	public void logout(String rawRefreshToken) {
		if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
			return;
		}

		Instant now = clock.instant();
		refreshTokens.findByTokenHash(tokenHashing.sha256(rawRefreshToken))
				.ifPresent((token) -> {
					token.revoke(now, null);
					refreshTokens.save(token);
				});
	}

	@Transactional(readOnly = true)
	public AuthenticatedUser me(UserPrincipal principal) {
		UserAccount user = users.findById(principal.id())
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Session is no longer valid."));
		return new AuthenticatedUser(user.id(), user.username(), user.email());
	}

	private TokenSession issueSession(UserAccount user, UUID familyId) {
		Instant now = clock.instant();
		Instant refreshExpiresAt = now.plus(jwtProperties.refreshExpiration());
		String rawRefreshToken = refreshTokenGenerator.generate();
		RefreshToken refreshToken = new RefreshToken(
				UUID.randomUUID(),
				user,
				tokenHashing.sha256(rawRefreshToken),
				familyId,
				now,
				refreshExpiresAt);
		refreshTokens.save(refreshToken);

		String accessToken = jwtService.issueAccessToken(new UserPrincipal(user.id(), user.username(), user.passwordHash(), user.enabled()));
		return new TokenSession(new AuthTokens(accessToken, rawRefreshToken, refreshExpiresAt), refreshToken);
	}

	private void markFamilyReused(UUID familyId, Instant now) {
		refreshTokens.findByFamilyId(familyId)
				.forEach((token) -> {
					token.markReuseDetected(now);
					refreshTokens.save(token);
				});
	}

	private record TokenSession(AuthTokens tokens, RefreshToken refreshToken) {
	}
}
