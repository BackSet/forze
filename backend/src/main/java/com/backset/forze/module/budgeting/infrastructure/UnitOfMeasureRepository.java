package com.backset.forze.module.budgeting.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.backset.forze.module.budgeting.domain.admin.UnitOfMeasure;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UnitOfMeasureRepository extends JpaRepository<UnitOfMeasure, UUID> {

	List<UnitOfMeasure> findByOrganizationId(UUID organizationId);

	Optional<UnitOfMeasure> findByOrganizationIdAndCode(UUID organizationId, String code);
}
