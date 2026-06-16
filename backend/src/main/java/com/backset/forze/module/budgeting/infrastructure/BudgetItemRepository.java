package com.backset.forze.module.budgeting.infrastructure;

import java.util.List;
import java.util.UUID;

import com.backset.forze.module.budgeting.domain.budget.BudgetItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetItemRepository extends JpaRepository<BudgetItem, UUID> {

	List<BudgetItem> findByBudgetVersionIdOrderByPosition(UUID budgetVersionId);

	List<BudgetItem> findByChapterId(UUID chapterId);
}
