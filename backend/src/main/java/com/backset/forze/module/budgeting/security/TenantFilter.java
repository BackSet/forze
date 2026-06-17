package com.backset.forze.module.budgeting.security;

import java.io.IOException;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.backset.forze.module.budgeting.infrastructure.MembershipRepository;
import com.backset.forze.module.identity.infrastructure.UserPrincipal;
import com.backset.forze.shared.TenantContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class TenantFilter extends OncePerRequestFilter {

	private final MembershipRepository membershipRepository;

	public TenantFilter(MembershipRepository membershipRepository) {
		this.membershipRepository = membershipRepository;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		String path = request.getRequestURI();

		// Bypass paths that don't need tenant validation
		if (path.startsWith("/api/auth/") ||
				path.equals("/api/organizations") ||
				path.equals("/api/organizations/") ||
				path.startsWith("/actuator/") ||
				path.startsWith("/v3/api-docs") ||
				path.equals("/v3/api-docs")) {
			filterChain.doFilter(request, response);
			return;
		}

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			filterChain.doFilter(request, response);
			return;
		}

		Object principal = authentication.getPrincipal();
		if (!(principal instanceof UserPrincipal userPrincipal)) {
			filterChain.doFilter(request, response);
			return;
		}

		String orgIdHeader = request.getHeader("X-Organization-Id");
		if (orgIdHeader == null || orgIdHeader.isBlank()) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.setContentType("application/json");
			response.getWriter().write("{\"detail\": \"El encabezado X-Organization-Id es requerido.\"}");
			return;
		}

		try {
			UUID organizationId = UUID.fromString(orgIdHeader);
			boolean isMember = membershipRepository.existsByOrganizationIdAndUserId(organizationId, userPrincipal.id());
			if (!isMember) {
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
				response.setContentType("application/json");
				response.getWriter().write("{\"detail\": \"No tiene acceso a la organizacion solicitada.\"}");
				return;
			}

			TenantContext.setTenantId(organizationId);
			try {
				filterChain.doFilter(request, response);
			}
			finally {
				TenantContext.clear();
			}
		}
		catch (IllegalArgumentException e) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.setContentType("application/json");
			response.getWriter().write("{\"detail\": \"El encabezado X-Organization-Id no es un UUID valido.\"}");
		}
	}
}
