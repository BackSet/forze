package com.backset.forze.module.budgeting.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.backset.forze.module.budgeting.domain.admin.TaxConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaxConfigRepository extends JpaRepository<TaxConfig, UUID> {

	List<TaxConfig> findByOrganizationId(UUID organizationId);

	Optional<TaxConfig> findByOrganizationIdAndCode(UUID organizationId, String code);
}

