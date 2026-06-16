package com.backset.forze.module.budgeting.infrastructure;

import java.util.List;
import java.util.UUID;

import com.backset.forze.module.budgeting.domain.catalog.ApuMaestro;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApuMaestroRepository extends JpaRepository<ApuMaestro, UUID> {

	List<ApuMaestro> findByOrganizationId(UUID organizationId);
}
