package com.backset.forze.module.budgeting.admin.api;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import com.backset.forze.module.budgeting.admin.application.MembershipService;
import com.backset.forze.module.budgeting.domain.admin.Membership;
import com.backset.forze.shared.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members")
public class MembershipController {

	private final MembershipService membershipService;

	public MembershipController(MembershipService membershipService) {
		this.membershipService = membershipService;
	}

	@GetMapping
	@Operation(summary = "List all members in the active organization.")
	@PreAuthorize("@securityService.hasPermission('ADMINISTRACION_READ')")
	public List<MembershipService.MembershipDetails> listMembers() {
		UUID orgId = TenantContext.getRequiredTenantId();
		return membershipService.getMembers(orgId);
	}

	@PostMapping
	@Operation(summary = "Add a user to the active organization.")
	@PreAuthorize("@securityService.hasPermission('ADMINISTRACION_WRITE')")
	public MembershipDto addMember(@Valid @RequestBody AddMemberRequest request) {
		UUID orgId = TenantContext.getRequiredTenantId();
		Membership membership = membershipService.addMember(orgId, request.usernameOrEmail(), request.role());
		return new MembershipDto(membership.id(), membership.organizationId(), membership.userId(), membership.role());
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update member role in the active organization.")
	@PreAuthorize("@securityService.hasPermission('ADMINISTRACION_WRITE')")
	public MembershipDto updateMemberRole(@PathVariable UUID id, @Valid @RequestBody UpdateMemberRoleRequest request) {
		UUID orgId = TenantContext.getRequiredTenantId();
		Membership membership = membershipService.updateRole(orgId, id, request.role());
		return new MembershipDto(membership.id(), membership.organizationId(), membership.userId(), membership.role());
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Remove a member from the active organization.")
	@PreAuthorize("@securityService.hasPermission('ADMINISTRACION_WRITE')")
	public void removeMember(@PathVariable UUID id) {
		UUID orgId = TenantContext.getRequiredTenantId();
		membershipService.removeMember(orgId, id);
	}

	public record AddMemberRequest(
			@NotBlank String usernameOrEmail,
			@NotBlank String role
	) {}

	public record UpdateMemberRoleRequest(
			@NotBlank String role
	) {}

	public record MembershipDto(UUID id, UUID organizationId, UUID userId, String role) {}
}
