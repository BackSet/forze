package com.backset.forze.module.budgeting.admin.api;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import com.backset.forze.module.budgeting.admin.application.RoleService;
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
@RequestMapping("/api")
public class RoleController {

	private final RoleService roleService;

	public RoleController(RoleService roleService) {
		this.roleService = roleService;
	}

	@GetMapping("/permissions")
	@Operation(summary = "List all registered permissions.")
	@PreAuthorize("@securityService.hasPermission('ADMINISTRACION_READ')")
	public List<RoleService.PermissionDto> listPermissions() {
		return roleService.listPermissions();
	}

	@GetMapping("/roles")
	@Operation(summary = "List system and custom roles for the active organization.")
	@PreAuthorize("@securityService.hasPermission('ADMINISTRACION_READ')")
	public List<RoleService.RoleDto> listRoles() {
		return roleService.listRoles(TenantContext.getRequiredTenantId());
	}

	@PostMapping("/roles")
	@Operation(summary = "Create a custom role in the active organization.")
	@PreAuthorize("@securityService.hasPermission('ADMINISTRACION_WRITE')")
	public RoleService.RoleDto createRole(@Valid @RequestBody SaveRoleRequest request) {
		return roleService.createRole(TenantContext.getRequiredTenantId(), request.code(), request.name(), request.permissions());
	}

	@PutMapping("/roles/{id}")
	@Operation(summary = "Update a custom role's name and permissions.")
	@PreAuthorize("@securityService.hasPermission('ADMINISTRACION_WRITE')")
	public RoleService.RoleDto updateRole(@PathVariable UUID id, @Valid @RequestBody UpdateRoleRequest request) {
		return roleService.updateRole(TenantContext.getRequiredTenantId(), id, request.name(), request.permissions());
	}

	@DeleteMapping("/roles/{id}")
	@Operation(summary = "Delete a custom role.")
	@PreAuthorize("@securityService.hasPermission('ADMINISTRACION_WRITE')")
	public void deleteRole(@PathVariable UUID id) {
		roleService.deleteRole(TenantContext.getRequiredTenantId(), id);
	}

	public record SaveRoleRequest(
			@NotBlank String code,
			@NotBlank String name,
			Set<String> permissions
	) {}

	public record UpdateRoleRequest(
			String name,
			Set<String> permissions
	) {}
}
