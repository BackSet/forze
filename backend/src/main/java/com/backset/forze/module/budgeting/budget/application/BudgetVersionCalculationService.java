package com.backset.forze.module.budgeting.budget.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

import com.backset.forze.module.budgeting.domain.budget.Budget;
import com.backset.forze.module.budgeting.domain.budget.BudgetItem;
import com.backset.forze.module.budgeting.domain.budget.BudgetVersion;
import com.backset.forze.module.budgeting.domain.budget.ItemApu;
import com.backset.forze.module.budgeting.domain.budget.ItemApuComponent;
import com.backset.forze.module.budgeting.domain.budget.ViabilityStatus;
import com.backset.forze.module.budgeting.domain.project.Project;
import com.backset.forze.module.budgeting.infrastructure.BudgetItemRepository;
import com.backset.forze.module.budgeting.infrastructure.BudgetRepository;
import com.backset.forze.module.budgeting.infrastructure.BudgetVersionRepository;
import com.backset.forze.module.budgeting.infrastructure.ItemApuComponentRepository;
import com.backset.forze.module.budgeting.infrastructure.ItemApuRepository;
import com.backset.forze.module.budgeting.infrastructure.ProjectRepository;
import com.backset.forze.shared.api.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BudgetVersionCalculationService {

	private final BudgetVersionRepository versionRepository;
	private final BudgetRepository budgetRepository;
	private final BudgetItemRepository itemRepository;
	private final ItemApuRepository apuRepository;
	private final ItemApuComponentRepository componentRepository;
	private final ProjectRepository projectRepository;

	private final ApuCalculationService apuCalculationService;
	private final BudgetItemCalculationService itemCalculationService;
	private final ViabilityEvaluationService viabilityEvaluationService;

	public BudgetVersionCalculationService(
			BudgetVersionRepository versionRepository,
			BudgetRepository budgetRepository,
			BudgetItemRepository itemRepository,
			ItemApuRepository apuRepository,
			ItemApuComponentRepository componentRepository,
			ProjectRepository projectRepository,
			ApuCalculationService apuCalculationService,
			BudgetItemCalculationService itemCalculationService,
			ViabilityEvaluationService viabilityEvaluationService
	) {
		this.versionRepository = versionRepository;
		this.budgetRepository = budgetRepository;
		this.itemRepository = itemRepository;
		this.apuRepository = apuRepository;
		this.componentRepository = componentRepository;
		this.projectRepository = projectRepository;
		this.apuCalculationService = apuCalculationService;
		this.itemCalculationService = itemCalculationService;
		this.viabilityEvaluationService = viabilityEvaluationService;
	}

	@Transactional
	public BudgetVersion calculateVersion(UUID organizationId, UUID versionId) {
		BudgetVersion version = versionRepository.findById(versionId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Version de presupuesto no encontrada."));

		Budget budget = budgetRepository.findById(version.budgetId())
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Presupuesto no encontrado."));

		if (!budget.organizationId().equals(organizationId)) {
			throw new ApiException(HttpStatus.FORBIDDEN, "El presupuesto no pertenece a la organizacion activa.");
		}

		Project project = projectRepository.findById(budget.projectId())
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Proyecto no encontrado."));

		BigDecimal minMargin = project.minimumMargin() != null ? project.minimumMargin() : BigDecimal.ZERO;

		List<BudgetItem> items = itemRepository.findByBudgetVersionIdOrderByPosition(versionId);

		BigDecimal totalCost = BigDecimal.ZERO;
		BigDecimal totalSale = BigDecimal.ZERO;

		for (BudgetItem item : items) {
			ItemApu apu = apuRepository.findByBudgetItemId(item.id()).orElse(null);
			if (apu != null) {
				List<ItemApuComponent> components = componentRepository.findByItemApuIdOrderByPosition(apu.id());

				for (ItemApuComponent comp : components) {
					BigDecimal lineTotal = apuCalculationService.calculateComponentLineTotal(comp, apu.yield());
					comp.recordLineTotal(lineTotal);
					componentRepository.save(comp);
				}

				BigDecimal unitCost = apuCalculationService.calculateApuUnitCost(apu, components);
				item.recordPricing(unitCost, item.unitPrice(), item.totalCost(), item.totalSale(), item.margin());
			}

			itemCalculationService.calculateItemTotals(item, version.indirectRate(), version.contingencyRate(), version.utilityRate());
			itemRepository.save(item);

			if (item.totalCost() != null) {
				totalCost = totalCost.add(item.totalCost());
			}
			if (item.totalSale() != null) {
				totalSale = totalSale.add(item.totalSale());
			}
		}

		BigDecimal margin = BigDecimal.ZERO;
		if (totalSale.compareTo(BigDecimal.ZERO) > 0) {
			margin = totalSale.subtract(totalCost).divide(totalSale, 4, RoundingMode.HALF_UP);
		}

		BudgetVersion temp = new BudgetVersion(version.id(), version.budgetId(), version.versionNumber());
		temp.configureFinancials(version.targetAmount(), version.utilityRate(), version.indirectRate(), version.contingencyRate(), version.id(), version.validUntil());
		temp.recordCalculation(totalCost, totalSale, margin, ViabilityStatus.VIABLE);

		ViabilityStatus viability = viabilityEvaluationService.evaluateViability(temp, items, minMargin);

		version.recordCalculation(totalCost, totalSale, margin, viability);
		return versionRepository.save(version);
	}
}
