package com.backset.forze.module.budgeting.infrastructure;

import java.util.List;
import java.util.UUID;

import com.backset.forze.module.budgeting.domain.scenario.ScenarioOverride;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScenarioOverrideRepository extends JpaRepository<ScenarioOverride, UUID> {

	List<ScenarioOverride> findByScenarioId(UUID scenarioId);
}
