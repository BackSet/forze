package com.backset.forze.module.budgeting.admin.application;

import java.util.List;
import java.util.UUID;

import com.backset.forze.module.budgeting.domain.admin.Membership;
import com.backset.forze.module.budgeting.domain.admin.MembershipRole;
import com.backset.forze.module.budgeting.domain.admin.Organization;
import com.backset.forze.module.budgeting.infrastructure.MembershipRepository;
import com.backset.forze.module.budgeting.infrastructure.OrganizationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationService {

	private final OrganizationRepository organizationRepository;
	private final MembershipRepository membershipRepository;

	public OrganizationService(OrganizationRepository organizationRepository, MembershipRepository membershipRepository) {
		this.organizationRepository = organizationRepository;
		this.membershipRepository = membershipRepository;
	}

	@Transactional
	public Organization createOrganization(String name, UUID creatorUserId) {
		UUID orgId = UUID.randomUUID();
		Organization organization = new Organization(orgId, name);
		organizationRepository.save(organization);

		Membership membership = new Membership(UUID.randomUUID(), orgId, creatorUserId, MembershipRole.ADMINISTRADOR);
		membershipRepository.save(membership);

		return organization;
	}

	@Transactional(readOnly = true)
	public List<Organization> getUserOrganizations(UUID userId) {
		List<Membership> memberships = membershipRepository.findByUserId(userId);
		List<UUID> orgIds = memberships.stream().map(Membership::organizationId).toList();
		return organizationRepository.findAllById(orgIds);
	}
}
