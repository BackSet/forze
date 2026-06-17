package com.backset.forze.module.budgeting.infrastructure;

import java.util.List;
import java.util.UUID;

import com.backset.forze.module.budgeting.domain.budget.BudgetRisk;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetRiskRepository extends JpaRepository<BudgetRisk, UUID> {

	List<BudgetRisk> findByBudgetVersionId(UUID budgetVersionId);
}
