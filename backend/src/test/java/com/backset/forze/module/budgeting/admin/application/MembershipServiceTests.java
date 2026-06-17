package com.backset.forze.module.budgeting.admin.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.backset.forze.module.budgeting.domain.admin.Membership;
import com.backset.forze.module.budgeting.domain.admin.Role;
import com.backset.forze.module.budgeting.infrastructure.MembershipRepository;
import com.backset.forze.module.budgeting.infrastructure.RoleRepository;
import com.backset.forze.module.identity.infrastructure.UserAccountRepository;
import com.backset.forze.shared.api.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class MembershipServiceTests {

	private MembershipRepository memberships;
	private UserAccountRepository users;
	private RoleRepository roles;
	private MembershipService service;

	private final UUID org = UUID.randomUUID();

	@BeforeEach
	void setUp() {
		memberships = Mockito.mock(MembershipRepository.class);
		users = Mockito.mock(UserAccountRepository.class);
		roles = Mockito.mock(RoleRepository.class);
		service = new MembershipService(memberships, users, roles);
		// All canonical role codes resolve as system roles.
		when(roles.findByOrganizationIdAndCode(any(), any())).thenReturn(Optional.empty());
		when(roles.findByOrganizationIdIsNullAndCode(any()))
				.thenReturn(Optional.of(new Role(UUID.randomUUID(), null, "X", "X", true, false)));
	}

	private Membership admin(UUID id) {
		return new Membership(id, org, UUID.randomUUID(), "ADMINISTRADOR");
	}

	@Test
	void cannotDemoteLastAdministrator() {
		UUID id = UUID.randomUUID();
		Membership lastAdmin = admin(id);
		when(memberships.findById(id)).thenReturn(Optional.of(lastAdmin));
		when(memberships.findByOrganizationId(org)).thenReturn(List.of(lastAdmin));

		assertThatThrownBy(() -> service.updateRole(org, id, "PRESUPUESTISTA"))
				.isInstanceOf(ApiException.class)
				.hasMessageContaining("ultimo administrador");
		verify(memberships, never()).save(any());
	}

	@Test
	void cannotRemoveLastAdministrator() {
		UUID id = UUID.randomUUID();
		Membership lastAdmin = admin(id);
		when(memberships.findById(id)).thenReturn(Optional.of(lastAdmin));
		when(memberships.findByOrganizationId(org)).thenReturn(List.of(lastAdmin));

		assertThatThrownBy(() -> service.removeMember(org, id))
				.isInstanceOf(ApiException.class)
				.hasMessageContaining("ultimo administrador");
		verify(memberships, never()).delete(any());
	}

	@Test
	void canDemoteAdministratorWhenAnotherExists() {
		UUID id = UUID.randomUUID();
		Membership a1 = admin(id);
		Membership a2 = admin(UUID.randomUUID());
		when(memberships.findById(id)).thenReturn(Optional.of(a1));
		when(memberships.findByOrganizationId(org)).thenReturn(List.of(a1, a2));
		when(memberships.save(any())).thenAnswer(inv -> inv.getArgument(0));

		Membership updated = service.updateRole(org, id, "PRESUPUESTISTA");

		assertThat(updated.role()).isEqualTo("PRESUPUESTISTA");
		verify(memberships).save(a1);
	}

	@Test
	void rejectsUnknownRole() {
		when(roles.findByOrganizationIdIsNullAndCode(eq("GHOST"))).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.addMember(org, "user", "GHOST"))
				.isInstanceOf(ApiException.class)
				.hasMessageContaining("no existe");
	}
}
