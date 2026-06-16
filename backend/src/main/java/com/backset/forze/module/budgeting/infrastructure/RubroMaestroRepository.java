package com.backset.forze.module.budgeting.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.backset.forze.module.budgeting.domain.catalog.RubroMaestro;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RubroMaestroRepository extends JpaRepository<RubroMaestro, UUID> {

	List<RubroMaestro> findByOrganizationId(UUID organizationId);

	Optional<RubroMaestro> findByOrganizationIdAndCode(UUID organizationId, String code);
}
