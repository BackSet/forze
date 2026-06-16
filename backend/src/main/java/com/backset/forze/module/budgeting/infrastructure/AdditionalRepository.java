package com.backset.forze.module.budgeting.infrastructure;

import java.util.List;
import java.util.UUID;

import com.backset.forze.module.budgeting.domain.control.Additional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdditionalRepository extends JpaRepository<Additional, UUID> {

	List<Additional> findByBaselineId(UUID baselineId);
}
