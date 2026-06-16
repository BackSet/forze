package com.backset.forze.module.budgeting.infrastructure;

import java.util.List;
import java.util.UUID;

import com.backset.forze.module.budgeting.domain.catalog.ApuComponent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApuComponentRepository extends JpaRepository<ApuComponent, UUID> {

	List<ApuComponent> findByApuMaestroIdOrderByPosition(UUID apuMaestroId);
}
