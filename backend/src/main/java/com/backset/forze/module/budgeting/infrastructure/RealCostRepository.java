package com.backset.forze.module.budgeting.infrastructure;

import java.util.List;
import java.util.UUID;

import com.backset.forze.module.budgeting.domain.control.RealCost;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RealCostRepository extends JpaRepository<RealCost, UUID> {

	List<RealCost> findByBaselineId(UUID baselineId);
}
