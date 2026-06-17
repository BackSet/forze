package com.backset.forze.module.budgeting.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.backset.forze.module.budgeting.domain.admin.Permission;
import com.backset.forze.module.budgeting.domain.admin.Role;
import com.backset.forze.module.budgeting.infrastructure.MembershipRepository;
import com.backset.forze.module.budgeting.infrastructure.PermissionRepository;
import com.backset.forze.module.budgeting.infrastructure.RoleRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.backset.forze.shared.TenantContext;

class SecurityServiceTests {

	private MembershipRepository memberships;
	private RoleRepository roles;
	private PermissionRepository permissions;
	private SecurityService service;

	private final UUID org = UUID.randomUUID();

	@BeforeEach
	void setUp() {
		memberships = Mockito.mock(MembershipRepository.class);
		roles = Mockito.mock(RoleRepository.class);
		permissions = Mockito.mock(PermissionRepository.class);
		service = new SecurityService(memberships, roles, permissions);
	}

	@AfterEach
	void tearDown() {
		TenantContext.clear();
	}

	private Permission perm(String code) {
		return new Permission(UUID.randomUUID(), code, "AREA", code);
	}

	@Test
	void administratorRoleGrantsEveryRegisteredPermission() {
		Role admin = new Role(UUID.randomUUID(), null, "ADMINISTRADOR", "Administrador", true, true);
		when(roles.findByOrganizationIdAndCode(any(), any())).thenReturn(Optional.empty());
		when(roles.findByOrganizationIdIsNullAndCode("ADMINISTRADOR")).thenReturn(Optional.of(admin));
		when(permissions.findAll()).thenReturn(List.of(perm("A"), perm("B"), perm("NEW_FUTURE_PERMISSION")));

		assertThat(service.effectivePermissionCodes(org, "ADMINISTRADOR"))
				.containsExactlyInAnyOrder("A", "B", "NEW_FUTURE_PERMISSION");
	}

	@Test
	void nonAdminRoleGrantsOnlyMappedPermissions() {
		Role role = new Role(UUID.randomUUID(), null, "APROBADOR", "Aprobador", true, false);
		role.replacePermissions(java.util.Set.of(perm("APROBACIONES_READ"), perm("APROBACIONES_WRITE")));
		when(roles.findByOrganizationIdAndCode(any(), any())).thenReturn(Optional.empty());
		when(roles.findByOrganizationIdIsNullAndCode("APROBADOR")).thenReturn(Optional.of(role));

		assertThat(service.effectivePermissionCodes(org, "APROBADOR"))
				.containsExactlyInAnyOrder("APROBACIONES_READ", "APROBACIONES_WRITE");
	}

	@Test
	void deniesWhenNoActiveTenant() {
		TenantContext.clear();
		assertThat(service.hasPermission("PROYECTOS_READ")).isFalse();
	}

	@Test
	void customOrgRoleTakesPrecedenceOverSystemRole() {
		Role custom = new Role(UUID.randomUUID(), org, "APROBADOR", "Custom", false, false);
		custom.replacePermissions(java.util.Set.of(perm("PROYECTOS_READ")));
		when(roles.findByOrganizationIdAndCode(org, "APROBADOR")).thenReturn(Optional.of(custom));

		assertThat(service.effectivePermissionCodes(org, "APROBADOR"))
				.containsExactly("PROYECTOS_READ");
	}
}
