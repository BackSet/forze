package com.backset.forze.module.budgeting.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.backset.forze.module.budgeting.domain.admin.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, UUID> {

	Optional<Role> findByOrganizationIdIsNullAndCode(String code);

	Optional<Role> findByOrganizationIdAndCode(UUID organizationId, String code);

	List<Role> findByOrganizationIdIsNull();

	List<Role> findByOrganizationId(UUID organizationId);
}
