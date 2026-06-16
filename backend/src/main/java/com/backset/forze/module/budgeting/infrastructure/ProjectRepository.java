package com.backset.forze.module.budgeting.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.backset.forze.module.budgeting.domain.project.Project;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

	List<Project> findByOrganizationId(UUID organizationId);

	Optional<Project> findByOrganizationIdAndCode(UUID organizationId, String code);
}
