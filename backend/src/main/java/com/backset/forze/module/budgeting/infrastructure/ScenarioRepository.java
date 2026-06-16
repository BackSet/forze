package com.backset.forze.module.budgeting.infrastructure;

import java.util.List;
import java.util.UUID;

import com.backset.forze.module.budgeting.domain.scenario.Scenario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScenarioRepository extends JpaRepository<Scenario, UUID> {

	List<Scenario> findByBudgetVersionId(UUID budgetVersionId);
}
