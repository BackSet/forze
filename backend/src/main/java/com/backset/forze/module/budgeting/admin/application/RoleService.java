package com.backset.forze.module.budgeting.admin.application;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.backset.forze.module.budgeting.domain.admin.MembershipRole;
import com.backset.forze.module.budgeting.domain.admin.Permission;
import com.backset.forze.module.budgeting.domain.admin.Role;
import com.backset.forze.module.budgeting.infrastructure.MembershipRepository;
import com.backset.forze.module.budgeting.infrastructure.PermissionRepository;
import com.backset.forze.module.budgeting.infrastructure.RoleRepository;
import com.backset.forze.shared.api.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Administration of RBAC roles. System roles are read-only; custom roles are
 * scoped to the active organization. The ADMINISTRADOR (all-permissions) role
 * cannot be edited or deleted, preserving the rule that it always has every
 * permission.
 */
@Service
public class RoleService {

	private final RoleRepository roleRepository;
	private final PermissionRepository permissionRepository;
	private final MembershipRepository membershipRepository;

	public RoleService(RoleRepository roleRepository, PermissionRepository permissionRepository,
			MembershipRepository membershipRepository) {
		this.roleRepository = roleRepository;
		this.permissionRepository = permissionRepository;
		this.membershipRepository = membershipRepository;
	}

	@Transactional(readOnly = true)
	public List<PermissionDto> listPermissions() {
		return permissionRepository.findAll().stream()
				.sorted((a, b) -> a.code().compareTo(b.code()))
				.map(p -> new PermissionDto(p.code(), p.area(), p.description()))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<RoleDto> listRoles(UUID organizationId) {
		List<RoleDto> result = new ArrayList<>();
		roleRepository.findByOrganizationIdIsNull().forEach(r -> result.add(toDto(r)));
		roleRepository.findByOrganizationId(organizationId).forEach(r -> result.add(toDto(r)));
		return result;
	}

	@Transactional
	public RoleDto createRole(UUID organizationId, String code, String name, Set<String> permissionCodes) {
		String normalizedCode = normalizeCode(code);
		if (isSystemCode(normalizedCode)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "El codigo coincide con un rol del sistema.");
		}
		if (roleRepository.findByOrganizationIdAndCode(organizationId, normalizedCode).isPresent()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Ya existe un rol con ese codigo en la organizacion.");
		}
		Role role = new Role(UUID.randomUUID(), organizationId, normalizedCode, name, false, false);
		role.replacePermissions(resolvePermissions(permissionCodes));
		return toDto(roleRepository.save(role));
	}

	@Transactional
	public RoleDto updateRole(UUID organizationId, UUID roleId, String name, Set<String> permissionCodes) {
		Role role = requireCustomRole(organizationId, roleId);
		if (name != null && !name.isBlank()) {
			role.rename(name);
		}
		role.replacePermissions(resolvePermissions(permissionCodes));
		return toDto(roleRepository.save(role));
	}

	@Transactional
	public void deleteRole(UUID organizationId, UUID roleId) {
		Role role = requireCustomRole(organizationId, roleId);
		boolean assigned = membershipRepository.findByOrganizationId(organizationId).stream()
				.anyMatch(m -> role.code().equals(m.role()));
		if (assigned) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "No se puede eliminar un rol asignado a miembros.");
		}
		roleRepository.delete(role);
	}

	private Role requireCustomRole(UUID organizationId, UUID roleId) {
		Role role = roleRepository.findById(roleId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Rol no encontrado."));
		if (role.isSystem() || role.organizationId() == null) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Los roles del sistema no se pueden modificar.");
		}
		if (!organizationId.equals(role.organizationId())) {
			throw new ApiException(HttpStatus.FORBIDDEN, "El rol no pertenece a la organizacion activa.");
		}
		return role;
	}

	private Set<Permission> resolvePermissions(Set<String> permissionCodes) {
		Set<Permission> permissions = new LinkedHashSet<>();
		if (permissionCodes != null) {
			for (String code : permissionCodes) {
				permissions.add(permissionRepository.findByCode(code)
						.orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Permiso desconocido: " + code)));
			}
		}
		return permissions;
	}

	private RoleDto toDto(Role role) {
		Set<String> codes = role.grantsAllPermissions()
				? permissionRepository.findAll().stream().map(Permission::code)
						.collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new))
				: role.permissions().stream().map(Permission::code)
						.collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
		return new RoleDto(role.id(), role.code(), role.name(), role.isSystem(), role.grantsAllPermissions(), codes);
	}

	private String normalizeCode(String code) {
		if (code == null || code.isBlank()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "El codigo del rol es requerido.");
		}
		return code.trim().toUpperCase().replace(' ', '_');
	}

	private boolean isSystemCode(String code) {
		for (MembershipRole canonical : MembershipRole.values()) {
			if (canonical.name().equals(code)) {
				return true;
			}
		}
		return false;
	}

	public record PermissionDto(String code, String area, String description) {}

	public record RoleDto(UUID id, String code, String name, boolean system, boolean allPermissions, Set<String> permissions) {}
}
