package com.backset.forze.module.budgeting.budget.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.backset.forze.module.budgeting.catalog.application.CatalogService;
import com.backset.forze.module.budgeting.domain.catalog.ApuComponent;
import com.backset.forze.module.budgeting.domain.catalog.ApuMaestro;
import com.backset.forze.module.budgeting.domain.catalog.ComponentSection;
import com.backset.forze.module.budgeting.domain.catalog.RubroMaestro;
import com.backset.forze.module.budgeting.domain.budget.Budget;
import com.backset.forze.module.budgeting.domain.budget.BudgetItem;
import com.backset.forze.module.budgeting.domain.budget.BudgetVersion;
import com.backset.forze.module.budgeting.domain.budget.Chapter;
import com.backset.forze.module.budgeting.domain.budget.ItemApu;
import com.backset.forze.module.budgeting.domain.budget.ItemApuComponent;
import com.backset.forze.module.budgeting.domain.budget.Measurement;
import com.backset.forze.module.budgeting.infrastructure.BudgetItemRepository;
import com.backset.forze.module.budgeting.infrastructure.BudgetRepository;
import com.backset.forze.module.budgeting.infrastructure.BudgetVersionRepository;
import com.backset.forze.module.budgeting.infrastructure.ChapterRepository;
import com.backset.forze.module.budgeting.infrastructure.ItemApuComponentRepository;
import com.backset.forze.module.budgeting.infrastructure.ItemApuRepository;
import com.backset.forze.module.budgeting.infrastructure.MeasurementRepository;
import com.backset.forze.shared.api.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BudgetService {

	private final BudgetRepository budgetRepository;
	private final BudgetVersionRepository versionRepository;
	private final ChapterRepository chapterRepository;
	private final BudgetItemRepository itemRepository;
	private final ItemApuRepository itemApuRepository;
	private final ItemApuComponentRepository itemApuComponentRepository;
	private final MeasurementRepository measurementRepository;

	private final CatalogService catalogService;
	private final BudgetVersionCalculationService calculationService;

	public BudgetService(
			BudgetRepository budgetRepository,
			BudgetVersionRepository versionRepository,
			ChapterRepository chapterRepository,
			BudgetItemRepository itemRepository,
			ItemApuRepository itemApuRepository,
			ItemApuComponentRepository itemApuComponentRepository,
			MeasurementRepository measurementRepository,
			CatalogService catalogService,
			BudgetVersionCalculationService calculationService
	) {
		this.budgetRepository = budgetRepository;
		this.versionRepository = versionRepository;
		this.chapterRepository = chapterRepository;
		this.itemRepository = itemRepository;
		this.itemApuRepository = itemApuRepository;
		this.itemApuComponentRepository = itemApuComponentRepository;
		this.measurementRepository = measurementRepository;
		this.catalogService = catalogService;
		this.calculationService = calculationService;
	}

	// Budgets
	@Transactional(readOnly = true)
	public List<Budget> getBudgetsForProject(UUID projectId) {
		return budgetRepository.findByProjectId(projectId);
	}

	@Transactional
	public Budget createBudget(UUID orgId, UUID projectId, CreateBudgetCmd cmd) {
		UUID budgetId = UUID.randomUUID();
		Budget budget = new Budget(budgetId, orgId, projectId, cmd.code(), cmd.name(), cmd.currencyCode());
		Budget saved = budgetRepository.save(budget);

		// Automatically create first version
		BudgetVersion version = new BudgetVersion(UUID.randomUUID(), budgetId, 1);
		version.describe("Version 1", "Version inicial", "Creacion inicial", cmd.userId());
		versionRepository.save(version);

		return saved;
	}

	// Budget Versions
	@Transactional(readOnly = true)
	public List<BudgetVersion> getVersionsForBudget(UUID budgetId) {
		return versionRepository.findByBudgetIdOrderByVersionNumber(budgetId);
	}

	@Transactional(readOnly = true)
	public BudgetVersion getVersion(UUID versionId) {
		return versionRepository.findById(versionId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Version de presupuesto no encontrada."));
	}

	@Transactional
	public BudgetVersion configureFinancials(UUID orgId, UUID versionId, ConfigureFinancialsCmd cmd) {
		BudgetVersion version = getVersion(versionId);
		version.configureFinancials(cmd.targetAmount(), cmd.utilityRate(), cmd.indirectRate(), cmd.contingencyRate(), cmd.taxConfigId(), cmd.validUntil());
		return versionRepository.save(version);
	}

	@Transactional
	public BudgetVersion copyVersion(UUID orgId, UUID budgetId, UUID baseVersionId, String name, String changeReason, UUID userId) {
		BudgetVersion base = versionRepository.findById(baseVersionId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Version base no encontrada."));

		List<BudgetVersion> versions = versionRepository.findByBudgetIdOrderByVersionNumber(budgetId);
		int nextNum = versions.stream().mapToInt(BudgetVersion::versionNumber).max().orElse(0) + 1;

		BudgetVersion newVersion = new BudgetVersion(UUID.randomUUID(), budgetId, nextNum);
		newVersion.describe(name, base.description(), changeReason, userId);
		newVersion.configureFinancials(base.targetAmount(), base.utilityRate(), base.indirectRate(), base.contingencyRate(), base.taxConfigId(), base.validUntil());
		BudgetVersion savedVersion = versionRepository.save(newVersion);

		List<Chapter> chapters = chapterRepository.findByBudgetVersionIdOrderByPosition(baseVersionId);
		Map<UUID, UUID> chapterMapping = new HashMap<>();
		for (Chapter chap : chapters) {
			UUID newChapId = UUID.randomUUID();
			chapterMapping.put(chap.id(), newChapId);
		}

		for (Chapter chap : chapters) {
			UUID parentId = chap.parentChapterId() != null ? chapterMapping.get(chap.parentChapterId()) : null;
			Chapter newChap = new Chapter(chapterMapping.get(chap.id()), savedVersion.id(), chap.name(), chap.position());
			if (parentId != null) {
				newChap.setParent(parentId);
			}
			if (chap.code() != null) {
				newChap.setCode(chap.code());
			}
			chapterRepository.save(newChap);
		}

		List<BudgetItem> items = itemRepository.findByBudgetVersionIdOrderByPosition(baseVersionId);
		for (BudgetItem item : items) {
			UUID newItemId = UUID.randomUUID();
			UUID newChapId = item.chapterId() != null ? chapterMapping.get(item.chapterId()) : null;
			BudgetItem newItem = new BudgetItem(newItemId, savedVersion.id(), item.name(), item.unitId(), item.quantity(), item.position());
			newItem.linkSource(item.sourceRubroId(), item.code(), item.description(), item.categoryId());
			if (newChapId != null) {
				newItem.placeInChapter(newChapId);
			}
			newItem.recordPricing(item.unitCost(), item.unitPrice(), item.totalCost(), item.totalSale(), item.margin());
			if (item.priceLocked()) {
				newItem.lockPrice();
			}
			itemRepository.save(newItem);

			ItemApu apu = itemApuRepository.findByBudgetItemId(item.id()).orElse(null);
			if (apu != null) {
				ItemApu newApu = new ItemApu(UUID.randomUUID(), newItemId, apu.sourceApuId(), apu.yield());
				itemApuRepository.save(newApu);

				List<ItemApuComponent> comps = itemApuComponentRepository.findByItemApuIdOrderByPosition(apu.id());
				for (ItemApuComponent comp : comps) {
					ItemApuComponent newComp = new ItemApuComponent(UUID.randomUUID(), newApu.id(), comp.section(), comp.unitId(), comp.quantity(), comp.unitPrice(), comp.position());
					newComp.describe(comp.sourceInsumoId(), comp.description(), comp.yield(), comp.wasteFactor(), comp.priceSource());
					if (comp.priceLocked()) {
						newComp.lockPrice();
					}
					itemApuComponentRepository.save(newComp);
				}
			}

			List<Measurement> measurements = measurementRepository.findByBudgetItemIdOrderByPosition(item.id());
			for (Measurement m : measurements) {
				Measurement newM = new Measurement(UUID.randomUUID(), newItemId, m.position());
				newM.setDimensions(m.description(), m.length(), m.width(), m.height(), m.itemCount(), m.factor());
				newM.recordResult(m.formula(), m.result(), m.notes());
				measurementRepository.save(newM);
			}
		}

		return savedVersion;
	}

	// Chapters
	@Transactional(readOnly = true)
	public List<Chapter> getChapters(UUID versionId) {
		return chapterRepository.findByBudgetVersionIdOrderByPosition(versionId);
	}

	@Transactional
	public Chapter createChapter(UUID versionId, String name, UUID parentId) {
		List<Chapter> existing = chapterRepository.findByBudgetVersionIdOrderByPosition(versionId);
		int pos = existing.isEmpty() ? 1 : existing.get(existing.size() - 1).position() + 1;

		Chapter chapter = new Chapter(UUID.randomUUID(), versionId, name, pos);
		if (parentId != null) {
			chapter.setParent(parentId);
		}
		return chapterRepository.save(chapter);
	}

	// Budget Items
	@Transactional(readOnly = true)
	public List<BudgetItem> getItems(UUID versionId) {
		return itemRepository.findByBudgetVersionIdOrderByPosition(versionId);
	}

	@Transactional
	public BudgetItem addRubroToVersion(UUID orgId, UUID versionId, UUID rubroId, UUID chapterId, BigDecimal quantity) {
		RubroMaestro rubro = catalogService.getRubro(orgId, rubroId);
		List<BudgetItem> existing = itemRepository.findByBudgetVersionIdOrderByPosition(versionId);
		int position = existing.isEmpty() ? 1 : existing.get(existing.size() - 1).position() + 1;

		BudgetItem item = new BudgetItem(UUID.randomUUID(), versionId, rubro.name(), rubro.unitId(), quantity, position);
		item.linkSource(rubro.id(), rubro.code(), rubro.description(), rubro.categoryId());
		if (chapterId != null) {
			item.placeInChapter(chapterId);
		}
		BudgetItem savedItem = itemRepository.save(item);

		if (rubro.baseApuId() != null) {
			ApuMaestro apu = catalogService.getApu(orgId, rubro.baseApuId());
			ItemApu itemApu = new ItemApu(UUID.randomUUID(), savedItem.id(), apu.id(), apu.yield());
			itemApuRepository.save(itemApu);

			List<ApuComponent> components = catalogService.getApuComponents(orgId, apu.id());
			int compPos = 1;
			for (ApuComponent comp : components) {
				ItemApuComponent itemComp = new ItemApuComponent(UUID.randomUUID(), itemApu.id(), comp.section(), comp.unitId(), comp.quantity(), comp.unitPrice(), compPos++);
				itemComp.describe(comp.insumoId(), comp.description(), comp.yield(), comp.wasteFactor(), "CATALOG_SNAPSHOT");
				itemApuComponentRepository.save(itemComp);
			}
		}

		return savedItem;
	}

	@Transactional
	public BudgetItem updateItemQuantity(UUID orgId, UUID itemId, BigDecimal quantity) {
		BudgetItem item = itemRepository.findById(itemId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Rubro de presupuesto no encontrado."));
		item.changeQuantity(quantity);
		return itemRepository.save(item);
	}

	@Transactional
	public void deleteItem(UUID orgId, UUID itemId) {
		BudgetItem item = itemRepository.findById(itemId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Rubro de presupuesto no encontrado."));
		itemRepository.delete(item);
	}

	// APU and components
	@Transactional(readOnly = true)
	public ItemApu getItemApu(UUID itemId) {
		return itemApuRepository.findByBudgetItemId(itemId).orElse(null);
	}

	@Transactional
	public ItemApu updateItemApuYield(UUID orgId, UUID apuId, BigDecimal yield) {
		ItemApu apu = itemApuRepository.findById(apuId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "APU no encontrado."));
		apu.changeYield(yield);
		return itemApuRepository.save(apu);
	}

	@Transactional(readOnly = true)
	public List<ItemApuComponent> getItemApuComponents(UUID apuId) {
		return itemApuComponentRepository.findByItemApuIdOrderByPosition(apuId);
	}

	@Transactional
	public ItemApuComponent addComponentToItemApu(UUID orgId, UUID apuId, AddComponentCmd cmd) {
		List<ItemApuComponent> existing = itemApuComponentRepository.findByItemApuIdOrderByPosition(apuId);
		int position = existing.isEmpty() ? 1 : existing.get(existing.size() - 1).position() + 1;

		ItemApuComponent comp = new ItemApuComponent(UUID.randomUUID(), apuId, cmd.section(), cmd.unitId(), cmd.quantity(), cmd.unitPrice(), position);
		comp.describe(cmd.insumoId(), cmd.description(), cmd.yield(), cmd.wasteFactor(), "MANUAL_ADD");
		return itemApuComponentRepository.save(comp);
	}

	@Transactional
	public ItemApuComponent updateItemApuComponent(UUID orgId, UUID componentId, AddComponentCmd cmd) {
		ItemApuComponent comp = itemApuComponentRepository.findById(componentId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Componente de APU no encontrado."));

		ItemApuComponent updated = new ItemApuComponent(componentId, comp.itemApuId(), cmd.section(), cmd.unitId(), cmd.quantity(), cmd.unitPrice(), comp.position());
		updated.describe(cmd.insumoId(), cmd.description(), cmd.yield(), cmd.wasteFactor(), comp.priceSource());
		return itemApuComponentRepository.save(updated);
	}

	@Transactional
	public void removeItemApuComponent(UUID orgId, UUID componentId) {
		ItemApuComponent comp = itemApuComponentRepository.findById(componentId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Componente de APU no encontrado."));
		itemApuComponentRepository.delete(comp);
	}

	// Measurements
	@Transactional(readOnly = true)
	public List<Measurement> getItemMeasurements(UUID itemId) {
		return measurementRepository.findByBudgetItemIdOrderByPosition(itemId);
	}

	@Transactional
	public Measurement addMeasurement(UUID orgId, UUID itemId, AddMeasurementCmd cmd) {
		List<Measurement> existing = measurementRepository.findByBudgetItemIdOrderByPosition(itemId);
		int position = existing.isEmpty() ? 1 : existing.get(existing.size() - 1).position() + 1;

		Measurement measurement = new Measurement(UUID.randomUUID(), itemId, position);
		measurement.setDimensions(cmd.description(), cmd.length(), cmd.width(), cmd.height(), cmd.itemCount(), cmd.factor());

		// Calculate formula result: length * width * height * itemCount * factor
		BigDecimal result = BigDecimal.ONE;
		if (cmd.length() != null) result = result.multiply(cmd.length());
		if (cmd.width() != null) result = result.multiply(cmd.width());
		if (cmd.height() != null) result = result.multiply(cmd.height());
		if (cmd.itemCount() != null) result = result.multiply(cmd.itemCount());
		if (cmd.factor() != null) result = result.multiply(cmd.factor());

		measurement.recordResult(cmd.formula(), result, cmd.notes());
		return measurementRepository.save(measurement);
	}

	@Transactional
	public void deleteMeasurement(UUID orgId, UUID measurementId) {
		Measurement measurement = measurementRepository.findById(measurementId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Medicion no encontrada."));
		measurementRepository.delete(measurement);
	}

	// CMD Records
	public record CreateBudgetCmd(String code, String name, String currencyCode, UUID userId) {}

	public record ConfigureFinancialsCmd(
			BigDecimal targetAmount,
			BigDecimal utilityRate,
			BigDecimal indirectRate,
			BigDecimal contingencyRate,
			UUID taxConfigId,
			LocalDate validUntil
	) {}

	public record AddComponentCmd(
			ComponentSection section,
			UUID insumoId,
			String description,
			UUID unitId,
			BigDecimal quantity,
			BigDecimal yield,
			BigDecimal wasteFactor,
			BigDecimal unitPrice
	) {}

	public record AddMeasurementCmd(
			String description,
			BigDecimal length,
			BigDecimal width,
			BigDecimal height,
			BigDecimal itemCount,
			BigDecimal factor,
			String formula,
			String notes
	) {}
}
