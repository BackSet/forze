package com.backset.forze.module.identity.infrastructure;

import java.io.IOException;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@ConditionalOnProperty(prefix = "forze.identity", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtService jwtService;
	private final UserAccountRepository users;

	public JwtAuthenticationFilter(JwtService jwtService, UserAccountRepository users) {
		this.jwtService = jwtService;
		this.users = users;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String header = request.getHeader(HttpHeaders.AUTHORIZATION);

		if (header != null && header.startsWith("Bearer ") && SecurityContextHolder.getContext().getAuthentication() == null) {
			authenticate(header.substring(7));
		}

		filterChain.doFilter(request, response);
	}

	private void authenticate(String token) {
		try {
			UUID userId = jwtService.parseSubject(token);
			users.findById(userId)
					.filter((user) -> user.enabled())
					.ifPresent((user) -> {
						UserPrincipal principal = new UserPrincipal(user.id(), user.username(), user.passwordHash(), user.enabled());
						UsernamePasswordAuthenticationToken authentication =
								UsernamePasswordAuthenticationToken.authenticated(principal, token, principal.getAuthorities());
						SecurityContextHolder.getContext().setAuthentication(authentication);
					});
		}
		catch (RuntimeException ignored) {
			SecurityContextHolder.clearContext();
		}
	}
}
