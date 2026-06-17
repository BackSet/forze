package com.backset.forze.module.budgeting.scenario.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.backset.forze.module.budgeting.budget.application.BudgetVersionCalculationService;
import com.backset.forze.module.budgeting.domain.budget.BudgetVersion;
import com.backset.forze.module.budgeting.domain.budget.ItemApuComponent;
import com.backset.forze.module.budgeting.domain.scenario.Scenario;
import com.backset.forze.module.budgeting.domain.scenario.ScenarioOverride;
import com.backset.forze.module.budgeting.domain.scenario.ScenarioType;
import com.backset.forze.module.budgeting.infrastructure.BudgetVersionRepository;
import com.backset.forze.module.budgeting.infrastructure.ItemApuComponentRepository;
import com.backset.forze.module.budgeting.infrastructure.ScenarioOverrideRepository;
import com.backset.forze.module.budgeting.infrastructure.ScenarioRepository;
import com.backset.forze.shared.api.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScenarioService {

	private final ScenarioRepository scenarioRepository;
	private final ScenarioOverrideRepository overrideRepository;
	private final BudgetVersionRepository versionRepository;
	private final ItemApuComponentRepository componentRepository;
	private final ScenarioCalculationService calculationService;
	private final BudgetVersionCalculationService versionCalculationService;

	public ScenarioService(
			ScenarioRepository scenarioRepository,
			ScenarioOverrideRepository overrideRepository,
			BudgetVersionRepository versionRepository,
			ItemApuComponentRepository componentRepository,
			ScenarioCalculationService calculationService,
			BudgetVersionCalculationService versionCalculationService
	) {
		this.scenarioRepository = scenarioRepository;
		this.overrideRepository = overrideRepository;
		this.versionRepository = versionRepository;
		this.componentRepository = componentRepository;
		this.calculationService = calculationService;
		this.versionCalculationService = versionCalculationService;
	}

	@Transactional(readOnly = true)
	public List<Scenario> getScenarios(UUID versionId) {
		return scenarioRepository.findByBudgetVersionId(versionId);
	}

	@Transactional
	public Scenario createScenario(UUID orgId, UUID versionId, CreateScenarioCmd cmd) {
		UUID id = UUID.randomUUID();
		Scenario scenario = new Scenario(id, versionId, cmd.name(), cmd.type());
		scenario.configure(cmd.utilityRate(), cmd.indirectRate(), cmd.contingencyRate(), cmd.durationDays(), cmd.constructionMethod());

		Scenario saved = scenarioRepository.save(scenario);

		for (OverrideCmd oCmd : cmd.overrides()) {
			ScenarioOverride override = new ScenarioOverride(UUID.randomUUID(), id, oCmd.itemApuComponentId());
			override.apply(oCmd.supplierId(), oCmd.unitPrice(), oCmd.yield(), oCmd.wasteFactor());
			overrideRepository.save(override);
		}

		calculationService.calculateScenario(orgId, id);
		return saved;
	}

	@Transactional
	public Scenario makeScenarioPrimary(UUID orgId, UUID scenarioId) {
		Scenario scenario = scenarioRepository.findById(scenarioId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Escenario no encontrado."));

		BudgetVersion version = versionRepository.findById(scenario.budgetVersionId())
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Version de presupuesto no encontrada."));

		List<Scenario> scenarios = scenarioRepository.findByBudgetVersionId(version.id());
		for (Scenario s : scenarios) {
			if (s.id().equals(scenarioId)) {
				s.makePrimary();
			}
			else {
				s.unsetPrimary();
			}
			scenarioRepository.save(s);
		}

		List<ScenarioOverride> overrides = overrideRepository.findByScenarioId(scenarioId);
		for (ScenarioOverride over : overrides) {
			ItemApuComponent comp = componentRepository.findById(over.itemApuComponentId()).orElse(null);
			if (comp != null) {
				comp.describe(comp.sourceInsumoId(), comp.description(),
						over.yield() != null ? over.yield() : comp.yield(),
						over.wasteFactor() != null ? over.wasteFactor() : comp.wasteFactor(),
						over.supplierId() != null ? "OVERRIDE_SUPPLIER" : comp.priceSource());
				if (over.unitPrice() != null) {
					ItemApuComponent updated = new ItemApuComponent(comp.id(), comp.itemApuId(), comp.section(), comp.unitId(), comp.quantity(), over.unitPrice(), comp.position());
					updated.describe(comp.sourceInsumoId(), comp.description(),
							over.yield() != null ? over.yield() : comp.yield(),
							over.wasteFactor() != null ? over.wasteFactor() : comp.wasteFactor(),
							"OVERRIDE_PRICE");
					componentRepository.save(updated);
				}
				else {
					componentRepository.save(comp);
				}
			}
		}

		version.configureFinancials(version.targetAmount(),
				scenario.utilityRate() != null ? scenario.utilityRate() : version.utilityRate(),
				scenario.indirectRate() != null ? scenario.indirectRate() : version.indirectRate(),
				scenario.contingencyRate() != null ? scenario.contingencyRate() : version.contingencyRate(),
				null, version.validUntil());
		versionRepository.save(version);

		versionCalculationService.calculateVersion(orgId, version.id());

		return scenario;
	}

	public record CreateScenarioCmd(
			String name,
			ScenarioType type,
			BigDecimal utilityRate,
			BigDecimal indirectRate,
			BigDecimal contingencyRate,
			Integer durationDays,
			String constructionMethod,
			List<OverrideCmd> overrides
	) {}

	public record OverrideCmd(
			UUID itemApuComponentId,
			UUID supplierId,
			BigDecimal unitPrice,
			BigDecimal yield,
			BigDecimal wasteFactor
	) {}
}
