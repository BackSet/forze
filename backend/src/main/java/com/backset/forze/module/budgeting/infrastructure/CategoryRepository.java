package com.backset.forze.module.budgeting.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.backset.forze.module.budgeting.domain.admin.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

	List<Category> findByOrganizationId(UUID organizationId);

	Optional<Category> findByOrganizationIdAndCode(UUID organizationId, String code);
}

