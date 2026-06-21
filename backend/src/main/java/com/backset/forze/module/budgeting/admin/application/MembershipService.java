package com.backset.forze.module.budgeting.admin.application;

import java.util.List;
import java.util.UUID;

import com.backset.forze.module.budgeting.audit.application.AuditService;
import com.backset.forze.module.budgeting.domain.admin.Membership;
import com.backset.forze.module.budgeting.domain.admin.MembershipRole;
import com.backset.forze.module.budgeting.infrastructure.MembershipRepository;
import com.backset.forze.module.budgeting.infrastructure.RoleRepository;
import com.backset.forze.module.identity.domain.UserAccount;
import com.backset.forze.module.identity.infrastructure.UserAccountRepository;
import com.backset.forze.shared.api.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MembershipService {

	private static final String ADMIN_ROLE = MembershipRole.ADMINISTRADOR.name();

	private final MembershipRepository membershipRepository;
	private final UserAccountRepository userAccountRepository;
	private final RoleRepository roleRepository;
	private final AuditService auditService;

	public MembershipService(MembershipRepository membershipRepository, UserAccountRepository userAccountRepository,
			RoleRepository roleRepository, AuditService auditService) {
		this.membershipRepository = membershipRepository;
		this.userAccountRepository = userAccountRepository;
		this.roleRepository = roleRepository;
		this.auditService = auditService;
	}

	@Transactional(readOnly = true)
	public List<MembershipDetails> getMembers(UUID organizationId) {
		List<Membership> memberships = membershipRepository.findByOrganizationId(organizationId);
		return memberships.stream().map(m -> {
			UserAccount user = userAccountRepository.findById(m.userId()).orElse(null);
			String username = user != null ? user.username() : "Unknown";
			String email = user != null ? user.email() : "";
			return new MembershipDetails(m.id(), m.organizationId(), m.userId(), username, email, m.role());
		}).toList();
	}

	@Transactional
	public Membership addMember(UUID organizationId, UUID actorUserId, String usernameOrEmail, String roleCode) {
		requireValidRole(organizationId, roleCode);

		UserAccount user = userAccountRepository.findByUsername(usernameOrEmail)
				.or(() -> userAccountRepository.findAll().stream()
						.filter(u -> usernameOrEmail.equalsIgnoreCase(u.email()))
						.findFirst())
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Usuario no encontrado."));

		if (membershipRepository.existsByOrganizationIdAndUserId(organizationId, user.id())) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "El usuario ya es miembro de esta organizacion.");
		}

		Membership membership = new Membership(UUID.randomUUID(), organizationId, user.id(), roleCode);
		Membership saved = membershipRepository.save(membership);
		auditService.log(organizationId, actorUserId, "ASSIGN_ROLE", "Membership", saved.id(),
				null, roleCode, "Miembro agregado a la organizacion", null);
		return saved;
	}

	@Transactional
	public Membership updateRole(UUID organizationId, UUID actorUserId, UUID membershipId, String roleCode) {
		requireValidRole(organizationId, roleCode);
		Membership membership = requireMembership(organizationId, membershipId);

		// Protect the last administrator: it cannot be demoted to a non-admin role.
		if (ADMIN_ROLE.equals(membership.role()) && !ADMIN_ROLE.equals(roleCode) && isLastAdmin(organizationId)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "No se puede degradar al ultimo administrador de la organizacion.");
		}

		String previousRole = membership.role();
		membership.changeRole(roleCode);
		Membership saved = membershipRepository.save(membership);
		auditService.log(organizationId, actorUserId, "UPDATE_ROLE", "Membership", saved.id(),
				previousRole, roleCode, "Rol de miembro actualizado", null);
		return saved;
	}

	@Transactional
	public void removeMember(UUID organizationId, UUID actorUserId, UUID membershipId) {
		Membership membership = requireMembership(organizationId, membershipId);

		if (ADMIN_ROLE.equals(membership.role()) && isLastAdmin(organizationId)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "No se puede eliminar al ultimo administrador de la organizacion.");
		}

		membershipRepository.delete(membership);
		auditService.log(organizationId, actorUserId, "REMOVE_MEMBER", "Membership", membership.id(),
				membership.role(), null, "Miembro removido de la organizacion", null);
	}

	private Membership requireMembership(UUID organizationId, UUID membershipId) {
		Membership membership = membershipRepository.findById(membershipId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Membresia no encontrada."));
		if (!membership.organizationId().equals(organizationId)) {
			throw new ApiException(HttpStatus.FORBIDDEN, "La membresia no pertenece a la organizacion activa.");
		}
		return membership;
	}

	private boolean isLastAdmin(UUID organizationId) {
		long adminCount = membershipRepository.findByOrganizationId(organizationId).stream()
				.filter(m -> ADMIN_ROLE.equals(m.role()))
				.count();
		return adminCount <= 1;
	}

	private void requireValidRole(UUID organizationId, String roleCode) {
		boolean exists = roleRepository.findByOrganizationIdAndCode(organizationId, roleCode).isPresent()
				|| roleRepository.findByOrganizationIdIsNullAndCode(roleCode).isPresent();
		if (!exists) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "El rol indicado no existe en la organizacion.");
		}
	}

	public record MembershipDetails(UUID id, UUID organizationId, UUID userId, String username, String email, String role) {}
}
