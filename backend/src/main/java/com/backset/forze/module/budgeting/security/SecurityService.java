package com.backset.forze.module.budgeting.security;

import java.util.Set;
import java.util.UUID;

import com.backset.forze.module.budgeting.domain.admin.MembershipRole;
import com.backset.forze.module.budgeting.infrastructure.MembershipRepository;
import com.backset.forze.module.identity.infrastructure.UserPrincipal;
import com.backset.forze.shared.TenantContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service("securityService")
public class SecurityService {

	private final MembershipRepository membershipRepository;

	public SecurityService(MembershipRepository membershipRepository) {
		this.membershipRepository = membershipRepository;
	}

	public boolean hasPermission(String permissionName) {
		ForzePermission permission = ForzePermission.valueOf(permissionName);
		UUID tenantId = TenantContext.getTenantId();
		if (tenantId == null) {
			return false;
		}

		if (SecurityContextHolder.getContext().getAuthentication() == null) {
			return false;
		}

		Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		if (!(principal instanceof UserPrincipal userPrincipal)) {
			return false;
		}

		UUID userId = userPrincipal.id();
		return membershipRepository.findByOrganizationIdAndUserId(tenantId, userId)
				.map(membership -> getPermissionsForRole(membership.role()).contains(permission))
				.orElse(false);
	}

	public Set<ForzePermission> getPermissionsForRole(MembershipRole role) {
		return switch (role) {
			case ADMINISTRADOR -> Set.of(ForzePermission.values());
			case PRESUPUESTISTA -> Set.of(
					ForzePermission.PROYECTOS_READ, ForzePermission.PROYECTOS_WRITE,
					ForzePermission.PRESUPUESTOS_READ, ForzePermission.PRESUPUESTOS_WRITE,
					ForzePermission.CATALOGOS_READ, ForzePermission.CATALOGOS_WRITE,
					ForzePermission.PROVEEDORES_READ,
					ForzePermission.APROBACIONES_READ,
					ForzePermission.DOCUMENTOS_READ, ForzePermission.DOCUMENTOS_WRITE,
					ForzePermission.AUDITORIA_READ
			);
			case APROBADOR -> Set.of(
					ForzePermission.PROYECTOS_READ,
					ForzePermission.PRESUPUESTOS_READ,
					ForzePermission.CATALOGOS_READ,
					ForzePermission.PROVEEDORES_READ,
					ForzePermission.APROBACIONES_READ, ForzePermission.APROBACIONES_WRITE,
					ForzePermission.DOCUMENTOS_READ,
					ForzePermission.AUDITORIA_READ
			);
			case COMPRAS -> Set.of(
					ForzePermission.PROYECTOS_READ,
					ForzePermission.PRESUPUESTOS_READ,
					ForzePermission.CATALOGOS_READ, ForzePermission.CATALOGOS_WRITE,
					ForzePermission.PROVEEDORES_READ, ForzePermission.PROVEEDORES_WRITE,
					ForzePermission.APROBACIONES_READ,
					ForzePermission.DOCUMENTOS_READ
			);
		};
	}
}
