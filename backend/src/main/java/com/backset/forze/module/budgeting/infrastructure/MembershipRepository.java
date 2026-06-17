package com.backset.forze.module.budgeting.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.backset.forze.module.budgeting.domain.admin.Membership;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipRepository extends JpaRepository<Membership, UUID> {

	Optional<Membership> findByOrganizationIdAndUserId(UUID organizationId, UUID userId);

	List<Membership> findByUserId(UUID userId);

	List<Membership> findByOrganizationId(UUID organizationId);

	boolean existsByOrganizationIdAndUserId(UUID organizationId, UUID userId);
}
