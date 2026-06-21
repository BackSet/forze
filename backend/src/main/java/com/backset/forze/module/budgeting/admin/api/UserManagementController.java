package com.backset.forze.module.budgeting.admin.api;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.backset.forze.module.budgeting.audit.application.AuditService;
import com.backset.forze.module.identity.domain.UserAccount;
import com.backset.forze.module.identity.infrastructure.UserAccountRepository;
import com.backset.forze.module.identity.infrastructure.UserPrincipal;
import com.backset.forze.shared.TenantContext;
import com.backset.forze.shared.api.ApiException;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
public class UserManagementController {

	private final UserAccountRepository users;
	private final PasswordEncoder passwordEncoder;
	private final AuditService auditService;

	public UserManagementController(UserAccountRepository users, PasswordEncoder passwordEncoder,
			AuditService auditService) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.auditService = auditService;
	}

	@GetMapping
	@Operation(summary = "List all users in the system.")
	@PreAuthorize("@securityService.hasPermission('ADMINISTRACION_READ')")
	public List<UserDto> listUsers() {
		return users.findAll().stream()
				.map(user -> new UserDto(user.id(), user.username(), user.email(), user.enabled()))
				.toList();
	}

	@PostMapping
	@Operation(summary = "Create a new user account.")
	@PreAuthorize("@securityService.hasPermission('ADMINISTRACION_WRITE')")
	@Transactional
	public UserDto createUser(@Valid @RequestBody CreateUserRequest request,
			@AuthenticationPrincipal UserPrincipal principal) {
		if (users.findByUsername(request.username()).isPresent()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "El nombre de usuario ya esta en uso.");
		}
		if (request.email() != null && !request.email().isBlank()) {
			if (users.findAll().stream().anyMatch(u -> request.email().equalsIgnoreCase(u.email()))) {
				throw new ApiException(HttpStatus.BAD_REQUEST, "El correo electronico ya esta en uso.");
			}
		}

		UUID id = UUID.randomUUID();
		String hash = passwordEncoder.encode(request.password());
		UserAccount newUser = new UserAccount(id, request.username(), request.email(), hash, true);
		users.save(newUser);

		// Audit records the username only; the password/hash are never logged.
		auditService.log(TenantContext.getTenantId(), principal.id(), "CREATE", "UserAccount", id,
				null, request.username(), "Cuenta de usuario creada", null);
		return new UserDto(id, request.username(), request.email(), true);
	}

	@PutMapping("/{id}/toggle")
	@Operation(summary = "Enable or disable a user account.")
	@PreAuthorize("@securityService.hasPermission('ADMINISTRACION_WRITE')")
	@Transactional
	public UserDto toggleUser(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
		UserAccount user = users.findById(id)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Usuario no encontrado."));

		UserAccount updated = new UserAccount(user.id(), user.username(), user.email(), user.passwordHash(), !user.enabled());
		users.save(updated);

		auditService.log(TenantContext.getTenantId(), principal.id(), "TOGGLE", "UserAccount", id,
				String.valueOf(user.enabled()), String.valueOf(updated.enabled()), "Estado de usuario actualizado", null);
		return new UserDto(updated.id(), updated.username(), updated.email(), updated.enabled());
	}

	public record CreateUserRequest(
			@NotBlank @Size(min = 3, max = 80) String username,
			@Email String email,
			@NotBlank @Size(min = 6) String password
	) {}

	public record UserDto(UUID id, String username, String email, boolean enabled) {}
}
