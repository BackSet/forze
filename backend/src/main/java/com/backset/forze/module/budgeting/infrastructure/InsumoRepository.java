package com.backset.forze.module.budgeting.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.backset.forze.module.budgeting.domain.catalog.Insumo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InsumoRepository extends JpaRepository<Insumo, UUID> {

	List<Insumo> findByOrganizationId(UUID organizationId);

	Optional<Insumo> findByOrganizationIdAndCode(UUID organizationId, String code);
}
