package com.backset.forze.module.budgeting.infrastructure;

import java.util.List;
import java.util.UUID;

import com.backset.forze.module.budgeting.domain.budget.Measurement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeasurementRepository extends JpaRepository<Measurement, UUID> {

	List<Measurement> findByBudgetItemIdOrderByPosition(UUID budgetItemId);
}
