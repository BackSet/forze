package com.backset.forze.module.budgeting.scenario.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.backset.forze.module.budgeting.budget.application.BudgetRounding;
import com.backset.forze.module.budgeting.domain.budget.BudgetItem;
import com.backset.forze.module.budgeting.domain.budget.BudgetVersion;
import com.backset.forze.module.budgeting.domain.budget.ItemApu;
import com.backset.forze.module.budgeting.domain.budget.ItemApuComponent;
import com.backset.forze.module.budgeting.domain.catalog.ComponentSection;
import com.backset.forze.module.budgeting.domain.scenario.Scenario;
import com.backset.forze.module.budgeting.domain.scenario.ScenarioOverride;
import com.backset.forze.module.budgeting.infrastructure.BudgetItemRepository;
import com.backset.forze.module.budgeting.infrastructure.BudgetVersionRepository;
import com.backset.forze.module.budgeting.infrastructure.ItemApuComponentRepository;
import com.backset.forze.module.budgeting.infrastructure.ItemApuRepository;
import com.backset.forze.module.budgeting.infrastructure.ScenarioOverrideRepository;
import com.backset.forze.module.budgeting.infrastructure.ScenarioRepository;
import com.backset.forze.shared.api.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScenarioCalculationService {

	private final ScenarioRepository scenarioRepository;
	private final ScenarioOverrideRepository overrideRepository;
	private final BudgetVersionRepository versionRepository;
	private final BudgetItemRepository itemRepository;
	private final ItemApuRepository apuRepository;
	private final ItemApuComponentRepository componentRepository;

	public ScenarioCalculationService(
			ScenarioRepository scenarioRepository,
			ScenarioOverrideRepository overrideRepository,
			BudgetVersionRepository versionRepository,
			BudgetItemRepository itemRepository,
			ItemApuRepository apuRepository,
			ItemApuComponentRepository componentRepository
	) {
		this.scenarioRepository = scenarioRepository;
		this.overrideRepository = overrideRepository;
		this.versionRepository = versionRepository;
		this.itemRepository = itemRepository;
		this.apuRepository = apuRepository;
		this.componentRepository = componentRepository;
	}

	@Transactional
	public Scenario calculateScenario(UUID orgId, UUID scenarioId) {
		Scenario scenario = scenarioRepository.findById(scenarioId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Escenario no encontrado."));

		BudgetVersion version = versionRepository.findById(scenario.budgetVersionId())
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Version de presupuesto base no encontrada."));

		List<ScenarioOverride> overrides = overrideRepository.findByScenarioId(scenarioId);
		Map<UUID, ScenarioOverride> overridesMap = overrides.stream()
				.collect(Collectors.toMap(ScenarioOverride::itemApuComponentId, o -> o));

		List<BudgetItem> items = itemRepository.findByBudgetVersionIdOrderByPosition(version.id());

		BigDecimal totalCost = BigDecimal.ZERO;
		BigDecimal totalSale = BigDecimal.ZERO;

		BigDecimal indirect = scenario.indirectRate() != null ? scenario.indirectRate() : version.indirectRate();
		BigDecimal contingency = scenario.contingencyRate() != null ? scenario.contingencyRate() : version.contingencyRate();
		BigDecimal utility = scenario.utilityRate() != null ? scenario.utilityRate() : version.utilityRate();

		for (BudgetItem item : items) {
			BigDecimal unitCost = item.unitCost();

			ItemApu apu = apuRepository.findByBudgetItemId(item.id()).orElse(null);
			if (apu != null) {
				List<ItemApuComponent> components = componentRepository.findByItemApuIdOrderByPosition(apu.id());
				BigDecimal apuYield = apu.yield() != null && apu.yield().compareTo(BigDecimal.ZERO) > 0 ? apu.yield() : BigDecimal.ONE;

				BigDecimal apuCost = BigDecimal.ZERO;
				for (ItemApuComponent comp : components) {
					ScenarioOverride over = overridesMap.get(comp.id());

					BigDecimal qty = comp.quantity();
					BigDecimal price = over != null && over.unitPrice() != null ? over.unitPrice() : comp.unitPrice();
					BigDecimal waste = over != null && over.wasteFactor() != null ? over.wasteFactor() : comp.wasteFactor();
					if (waste == null) {
						waste = BigDecimal.ZERO;
					}
					BigDecimal compYield = over != null && over.yield() != null ? over.yield() : comp.yield();

					BigDecimal lineTotal;
					BigDecimal lineBase = qty.multiply(price).multiply(BigDecimal.ONE.add(waste));
					if (comp.section() == ComponentSection.MANO_DE_OBRA || comp.section() == ComponentSection.EQUIPOS) {
						BigDecimal effectiveYield = compYield != null && compYield.compareTo(BigDecimal.ZERO) > 0 ? compYield : apuYield;
						lineTotal = BudgetRounding.money(BudgetRounding.divideUnit(lineBase, effectiveYield));
					}
					else {
						lineTotal = BudgetRounding.money(lineBase);
					}
					apuCost = apuCost.add(lineTotal);
				}
				unitCost = BudgetRounding.unit(apuCost);
			}

			if (unitCost == null) {
				unitCost = BigDecimal.ZERO;
			}

			BigDecimal itemQty = item.quantity() != null ? item.quantity() : BigDecimal.ZERO;
			BigDecimal itemCost = BudgetRounding.money(itemQty.multiply(unitCost));

			BigDecimal unitPrice = item.unitPrice();
			if (!item.priceLocked() || unitPrice == null) {
				BigDecimal sumRates = BigDecimal.ZERO;
				if (indirect != null) {
					sumRates = sumRates.add(indirect);
				}
				if (contingency != null) {
					sumRates = sumRates.add(contingency);
				}
				if (utility != null) {
					sumRates = sumRates.add(utility);
				}

				unitPrice = BudgetRounding.unit(unitCost.multiply(BigDecimal.ONE.add(sumRates)));
			}

			BigDecimal itemSale = BudgetRounding.money(itemQty.multiply(unitPrice));

			totalCost = totalCost.add(itemCost);
			totalSale = totalSale.add(itemSale);
		}

		BigDecimal margin = BigDecimal.ZERO;
		if (totalSale.compareTo(BigDecimal.ZERO) > 0) {
			margin = BudgetRounding.divideUnit(totalSale.subtract(totalCost), totalSale);
		}

		scenario.recordComparison(totalCost, totalSale, margin, scenario.risk());
		return scenarioRepository.save(scenario);
	}
}
