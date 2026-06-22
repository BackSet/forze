package com.backset.forze.module.budgeting.infrastructure;

import java.util.List;
import java.util.UUID;

import com.backset.forze.module.budgeting.domain.budget.Budget;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetRepository extends JpaRepository<Budget, UUID> {

	List<Budget> findByProjectId(UUID projectId);

	boolean existsByProjectIdAndCode(UUID projectId, String code);
}
