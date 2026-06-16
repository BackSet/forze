package com.backset.forze.module.budgeting.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.backset.forze.module.budgeting.domain.budget.BudgetVersion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetVersionRepository extends JpaRepository<BudgetVersion, UUID> {

	List<BudgetVersion> findByBudgetIdOrderByVersionNumber(UUID budgetId);

	Optional<BudgetVersion> findByBudgetIdAndVersionNumber(UUID budgetId, int versionNumber);
}
