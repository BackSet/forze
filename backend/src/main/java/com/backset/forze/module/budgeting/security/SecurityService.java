package com.backset.forze.module.budgeting.security;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.backset.forze.module.budgeting.domain.admin.Permission;
import com.backset.forze.module.budgeting.domain.admin.Role;
import com.backset.forze.module.budgeting.infrastructure.MembershipRepository;
import com.backset.forze.module.budgeting.infrastructure.PermissionRepository;
import com.backset.forze.module.budgeting.infrastructure.RoleRepository;
import com.backset.forze.module.identity.infrastructure.UserPrincipal;
import com.backset.forze.shared.TenantContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Backend authority for RBAC. Resolves a user's effective permissions in the
 * active organization by reading the persistent role/permission model. A role
 * flagged {@code allPermissions} (the ADMINISTRADOR system role) always grants
 * every registered permission, including ones added later.
 */
@Service("securityService")
public class SecurityService {

	private final MembershipRepository membershipRepository;
	private final RoleRepository roleRepository;
	private final PermissionRepository permissionRepository;

	public SecurityService(MembershipRepository membershipRepository, RoleRepository roleRepository,
			PermissionRepository permissionRepository) {
		this.membershipRepository = membershipRepository;
		this.roleRepository = roleRepository;
		this.permissionRepository = permissionRepository;
	}

	/** Used by {@code @PreAuthorize("@securityService.hasPermission('...')")}. */
	@Transactional(readOnly = true)
	public boolean hasPermission(String permissionName) {
		UUID tenantId = TenantContext.getTenantId();
		if (tenantId == null) {
			return false;
		}
		return currentUserId()
				.flatMap(userId -> membershipRepository.findByOrganizationIdAndUserId(tenantId, userId))
				.map(membership -> effectivePermissionCodes(tenantId, membership.role()).contains(permissionName))
				.orElse(false);
	}

	/** Effective permission codes for a role code in the given organization. */
	@Transactional(readOnly = true)
	public Set<String> effectivePermissionCodes(UUID organizationId, String roleCode) {
		Role role = resolveRole(organizationId, roleCode).orElse(null);
		if (role == null) {
			return Set.of();
		}
		if (role.grantsAllPermissions()) {
			return permissionRepository.findAll().stream().map(Permission::code).collect(Collectors.toSet());
		}
		return role.permissions().stream().map(Permission::code).collect(Collectors.toSet());
	}

	/** Resolve a role by code: an organization-scoped custom role takes precedence over a system role. */
	@Transactional(readOnly = true)
	public Optional<Role> resolveRole(UUID organizationId, String roleCode) {
		Optional<Role> custom = roleRepository.findByOrganizationIdAndCode(organizationId, roleCode);
		if (custom.isPresent()) {
			return custom;
		}
		return roleRepository.findByOrganizationIdIsNullAndCode(roleCode);
	}

	private Optional<UUID> currentUserId() {
		if (SecurityContextHolder.getContext().getAuthentication() == null) {
			return Optional.empty();
		}
		Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		if (principal instanceof UserPrincipal userPrincipal) {
			return Optional.of(userPrincipal.id());
		}
		return Optional.empty();
	}
}
