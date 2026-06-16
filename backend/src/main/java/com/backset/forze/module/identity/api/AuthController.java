package com.backset.forze.module.identity.api;

import com.backset.forze.configuration.SecurityProperties;
import com.backset.forze.module.identity.application.AuthService;
import com.backset.forze.module.identity.application.AuthTokens;
import com.backset.forze.module.identity.application.AuthenticatedUser;
import com.backset.forze.module.identity.infrastructure.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@ConditionalOnProperty(prefix = "forze.identity", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AuthController {

	private final AuthService authService;
	private final SecurityProperties securityProperties;

	public AuthController(AuthService authService, SecurityProperties securityProperties) {
		this.authService = authService;
		this.securityProperties = securityProperties;
	}

	@PostMapping("/login")
	@Operation(summary = "Authenticate with username and password.")
	public ResponseEntity<AuthTokenResponse> login(@Valid @RequestBody LoginRequest request) {
		AuthTokens tokens = authService.login(request.username(), request.password());
		return ResponseEntity.ok()
				.header(HttpHeaders.SET_COOKIE, refreshCookie(tokens.refreshToken(), false).toString())
				.body(new AuthTokenResponse(tokens.accessToken(), "Bearer"));
	}

	@PostMapping("/refresh")
	@Operation(summary = "Rotate the refresh token and issue a new access token.")
	public ResponseEntity<AuthTokenResponse> refresh(
			@CookieValue(name = "${forze.security.refresh-cookie-name}", required = false) String refreshToken
	) {
		AuthTokens tokens = authService.refresh(refreshToken);
		return ResponseEntity.ok()
				.header(HttpHeaders.SET_COOKIE, refreshCookie(tokens.refreshToken(), false).toString())
				.body(new AuthTokenResponse(tokens.accessToken(), "Bearer"));
	}

	@PostMapping("/logout")
	@Operation(summary = "Revoke the current refresh token.")
	public ResponseEntity<Void> logout(HttpServletRequest request) {
		authService.logout(refreshTokenFrom(request));
		return ResponseEntity.noContent()
				.header(HttpHeaders.SET_COOKIE, refreshCookie("", true).toString())
				.build();
	}

	@GetMapping("/me")
	@Operation(summary = "Return the current authenticated user.")
	public MeResponse me(@AuthenticationPrincipal UserPrincipal principal) {
		AuthenticatedUser user = authService.me(principal);
		return new MeResponse(user.id(), user.username(), user.email());
	}

	private ResponseCookie refreshCookie(String value, boolean clear) {
		return ResponseCookie.from(securityProperties.refreshCookieName(), value)
				.httpOnly(true)
				.secure(securityProperties.cookieSecure())
				.sameSite(securityProperties.cookieSameSite())
				.path("/api/auth")
				.maxAge(clear ? 0 : -1)
				.build();
	}

	private String refreshTokenFrom(HttpServletRequest request) {
		if (request.getCookies() == null) {
			return null;
		}
		for (Cookie cookie : request.getCookies()) {
			if (securityProperties.refreshCookieName().equals(cookie.getName())) {
				return cookie.getValue();
			}
		}
		return null;
	}
}
