package com.backset.forze.module.budgeting.admin.api;

import java.util.List;
import java.util.UUID;

import com.backset.forze.module.budgeting.infrastructure.MembershipRepository;
import com.backset.forze.module.budgeting.security.SecurityService;
import com.backset.forze.module.identity.infrastructure.UserPrincipal;
import com.backset.forze.shared.TenantContext;
import com.backset.forze.shared.api.ApiException;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the current user's effective role and permissions in the active
 * organization, so the frontend can gate UI by permission. The backend remains
 * the authority: this endpoint never relaxes server-side {@code @PreAuthorize}.
 */
@RestController
@RequestMapping("/api/me")
public class AccessController {

	private final MembershipRepository membershipRepository;
	private final SecurityService securityService;

	public AccessController(MembershipRepository membershipRepository, SecurityService securityService) {
		this.membershipRepository = membershipRepository;
		this.securityService = securityService;
	}

	@GetMapping("/access")
	@Operation(summary = "Current user's role and effective permissions in the active organization.")
	@PreAuthorize("isAuthenticated()")
	public AccessDto access(@AuthenticationPrincipal UserPrincipal principal) {
		UUID orgId = TenantContext.getRequiredTenantId();
		return membershipRepository.findByOrganizationIdAndUserId(orgId, principal.id())
				.map(m -> new AccessDto(orgId, m.role(),
						securityService.effectivePermissionCodes(orgId, m.role()).stream().sorted().toList()))
				.orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "No es miembro de la organizacion activa."));
	}

	public record AccessDto(UUID organizationId, String role, List<String> permissions) {}
}
