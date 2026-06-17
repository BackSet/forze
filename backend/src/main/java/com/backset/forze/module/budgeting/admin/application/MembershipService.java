package com.backset.forze.module.budgeting.admin.application;

import java.util.List;
import java.util.UUID;

import com.backset.forze.module.budgeting.domain.admin.Membership;
import com.backset.forze.module.budgeting.domain.admin.MembershipRole;
import com.backset.forze.module.budgeting.infrastructure.MembershipRepository;
import com.backset.forze.module.identity.domain.UserAccount;
import com.backset.forze.module.identity.infrastructure.UserAccountRepository;
import com.backset.forze.shared.api.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MembershipService {

	private final MembershipRepository membershipRepository;
	private final UserAccountRepository userAccountRepository;

	public MembershipService(MembershipRepository membershipRepository, UserAccountRepository userAccountRepository) {
		this.membershipRepository = membershipRepository;
		this.userAccountRepository = userAccountRepository;
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
	public Membership addMember(UUID organizationId, String usernameOrEmail, MembershipRole role) {
		UserAccount user = userAccountRepository.findByUsername(usernameOrEmail)
				.or(() -> userAccountRepository.findAll().stream()
						.filter(u -> usernameOrEmail.equalsIgnoreCase(u.email()))
						.findFirst())
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Usuario no encontrado."));

		if (membershipRepository.existsByOrganizationIdAndUserId(organizationId, user.id())) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "El usuario ya es miembro de esta organizacion.");
		}

		Membership membership = new Membership(UUID.randomUUID(), organizationId, user.id(), role);
		return membershipRepository.save(membership);
	}

	@Transactional
	public Membership updateRole(UUID organizationId, UUID membershipId, MembershipRole role) {
		Membership membership = membershipRepository.findById(membershipId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Membresia no encontrada."));

		if (!membership.organizationId().equals(organizationId)) {
			throw new ApiException(HttpStatus.FORBIDDEN, "La membresia no pertenece a la organizacion activa.");
		}

		membership.changeRole(role);
		return membershipRepository.save(membership);
	}

	@Transactional
	public void removeMember(UUID organizationId, UUID membershipId) {
		Membership membership = membershipRepository.findById(membershipId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Membresia no encontrada."));

		if (!membership.organizationId().equals(organizationId)) {
			throw new ApiException(HttpStatus.FORBIDDEN, "La membresia no pertenece a la organizacion activa.");
		}

		long adminCount = membershipRepository.findByOrganizationId(organizationId).stream()
				.filter(m -> m.role() == MembershipRole.ADMINISTRADOR)
				.count();

		if (membership.role() == MembershipRole.ADMINISTRADOR && adminCount <= 1) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "No se puede eliminar al ultimo administrador de la organizacion.");
		}

		membershipRepository.delete(membership);
	}

	public record MembershipDetails(UUID id, UUID organizationId, UUID userId, String username, String email, MembershipRole role) {}
}
