package com.backset.forze.module.budgeting.budget.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
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
import com.backset.forze.module.budgeting.domain.budget.BudgetRisk;
import com.backset.forze.module.budgeting.domain.supplier.PriceHistory;
import com.backset.forze.module.budgeting.domain.supplier.PriceStatus;
import com.backset.forze.module.budgeting.infrastructure.BudgetRiskRepository;
import com.backset.forze.module.budgeting.infrastructure.PriceHistoryRepository;
import com.backset.forze.module.budgeting.audit.application.AuditService;
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
	private final BudgetRiskRepository budgetRiskRepository;
	private final PriceHistoryRepository priceHistoryRepository;
	private final ApuCalculationService apuCalculationService;
	private final AuditService auditService;
	private final AlertGenerationService alertGenerationService;

	public BudgetService(
			BudgetRepository budgetRepository,
			BudgetVersionRepository versionRepository,
			ChapterRepository chapterRepository,
			BudgetItemRepository itemRepository,
			ItemApuRepository itemApuRepository,
			ItemApuComponentRepository itemApuComponentRepository,
			MeasurementRepository measurementRepository,
			CatalogService catalogService,
			BudgetVersionCalculationService calculationService,
			BudgetRiskRepository budgetRiskRepository,
			PriceHistoryRepository priceHistoryRepository,
			ApuCalculationService apuCalculationService,
			AuditService auditService,
			AlertGenerationService alertGenerationService
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
		this.budgetRiskRepository = budgetRiskRepository;
		this.priceHistoryRepository = priceHistoryRepository;
		this.apuCalculationService = apuCalculationService;
		this.auditService = auditService;
		this.alertGenerationService = alertGenerationService;
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

	// Risk management
	@Transactional(readOnly = true)
	public List<BudgetRisk> getRisks(UUID versionId) {
		return budgetRiskRepository.findByBudgetVersionId(versionId);
	}

	@Transactional(readOnly = true)
	public BudgetRisk getRisk(UUID orgId, UUID riskId) {
		return budgetRiskRepository.findById(riskId)
				.filter(r -> r.organizationId().equals(orgId))
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Risk not found"));
	}

	@Transactional
	public BudgetRisk addRisk(UUID orgId, UUID versionId, CreateRiskCmd cmd, UUID userId) {
		BudgetVersion version = getVersion(versionId);
		if (version.isApproved()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot add risks to an approved budget version");
		}

		UUID id = UUID.randomUUID();
		BudgetRisk risk = new BudgetRisk(id, orgId, versionId, cmd.description(), cmd.probability(), cmd.impact());
		risk.updateDetails(cmd.description(), cmd.probability(), cmd.impact(), cmd.assignedTo(), cmd.mitigation(), cmd.mitigated());
		BudgetRisk saved = budgetRiskRepository.save(risk);

		auditService.log(orgId, userId, "CREATE", "BudgetRisk", id, null, cmd.description(), "Risk added", null);

		return saved;
	}

	@Transactional
	public BudgetRisk updateRisk(UUID orgId, UUID riskId, CreateRiskCmd cmd, UUID userId) {
		BudgetRisk risk = getRisk(orgId, riskId);
		BudgetVersion version = getVersion(risk.budgetVersionId());
		if (version.isApproved()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot update risks on an approved budget version");
		}

		String oldValue = risk.description() + " (Prob: " + risk.probability() + ", Imp: " + risk.impact() + ")";
		risk.updateDetails(cmd.description(), cmd.probability(), cmd.impact(), cmd.assignedTo(), cmd.mitigation(), cmd.mitigated());
		BudgetRisk saved = budgetRiskRepository.save(risk);

		String newValue = cmd.description() + " (Prob: " + cmd.probability() + ", Imp: " + cmd.impact() + ")";
		auditService.log(orgId, userId, "UPDATE", "BudgetRisk", riskId, oldValue, newValue, "Risk updated", null);

		return saved;
	}

	@Transactional
	public void deleteRisk(UUID orgId, UUID riskId, UUID userId) {
		BudgetRisk risk = getRisk(orgId, riskId);
		BudgetVersion version = getVersion(risk.budgetVersionId());
		if (version.isApproved()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot delete risks from an approved budget version");
		}

		budgetRiskRepository.delete(risk);
		auditService.log(orgId, userId, "DELETE", "BudgetRisk", riskId, risk.description(), null, "Risk deleted", null);
	}

	// Price application
	@Transactional(readOnly = true)
	public PriceUpdatePreviewDto getPriceUpdatePreview(UUID orgId, UUID versionId) {
		BudgetVersion version = getVersion(versionId);
		List<BudgetItem> items = getItems(versionId);

		List<PriceChangeDto> changes = new ArrayList<>();
		BigDecimal proposedVersionTotalCost = BigDecimal.ZERO;

		for (BudgetItem item : items) {
			BigDecimal itemProposedCost = item.unitCost() != null ? item.unitCost() : BigDecimal.ZERO;
			ItemApu apu = itemApuRepository.findByBudgetItemId(item.id()).orElse(null);
			if (apu != null) {
				List<ItemApuComponent> components = itemApuComponentRepository.findByItemApuIdOrderByPosition(apu.id());
				BigDecimal apuYield = apu.yield() != null && apu.yield().compareTo(BigDecimal.ZERO) > 0 ? apu.yield() : BigDecimal.ONE;

				BigDecimal apuProposedUnitCost = BigDecimal.ZERO;
				for (ItemApuComponent comp : components) {
					BigDecimal currentLineTotal = comp.lineTotal() != null ? comp.lineTotal() : BigDecimal.ZERO;
					BigDecimal proposedLineTotal = currentLineTotal;

					if (!comp.priceLocked() && comp.sourceInsumoId() != null) {
						PriceHistory latestPrice = priceHistoryRepository.findByInsumoIdOrderByPriceDateDesc(comp.sourceInsumoId()).stream()
								.filter(ph -> ph.organizationId().equals(orgId) && ph.status() == PriceStatus.VIGENTE)
								.findFirst()
								.orElse(null);

						if (latestPrice != null && latestPrice.price().compareTo(comp.unitPrice()) != 0) {
							ItemApuComponent tempComp = new ItemApuComponent(comp.id(), comp.itemApuId(), comp.section(), comp.unitId(), comp.quantity(), latestPrice.price(), comp.position());
							tempComp.describe(comp.sourceInsumoId(), comp.description(), comp.yield(), comp.wasteFactor(), comp.priceSource());
							proposedLineTotal = apuCalculationService.calculateComponentLineTotal(tempComp, apuYield);

							BigDecimal priceDiff = latestPrice.price().subtract(comp.unitPrice());
							BigDecimal lineDiff = proposedLineTotal.subtract(currentLineTotal);

							var insumo = catalogService.getInsumo(orgId, comp.sourceInsumoId());

							changes.add(new PriceChangeDto(
									comp.id(),
									comp.sourceInsumoId(),
									insumo.code(),
									insumo.name(),
									comp.description(),
									comp.unitPrice(),
									latestPrice.price(),
									priceDiff,
									currentLineTotal,
									proposedLineTotal,
									lineDiff
							));
						}
					}
					apuProposedUnitCost = apuProposedUnitCost.add(proposedLineTotal);
				}
				itemProposedCost = apuProposedUnitCost.setScale(4, RoundingMode.HALF_UP);
			}

			BigDecimal proposedItemCost = item.quantity().multiply(itemProposedCost).setScale(2, RoundingMode.HALF_UP);
			proposedVersionTotalCost = proposedVersionTotalCost.add(proposedItemCost);
		}

		BigDecimal currentTotal = version.totalCost() != null ? version.totalCost() : BigDecimal.ZERO;
		BigDecimal difference = proposedVersionTotalCost.subtract(currentTotal);

		return new PriceUpdatePreviewDto(changes, currentTotal, proposedVersionTotalCost, difference);
	}

	@Transactional
	public void applyNewPrices(UUID orgId, UUID versionId, UUID userId) {
		BudgetVersion version = getVersion(versionId);
		if (version.isApproved()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot update prices on an approved budget version");
		}

		List<BudgetItem> items = getItems(versionId);
		int appliedCount = 0;

		for (BudgetItem item : items) {
			ItemApu apu = itemApuRepository.findByBudgetItemId(item.id()).orElse(null);
			if (apu != null) {
				List<ItemApuComponent> components = itemApuComponentRepository.findByItemApuIdOrderByPosition(apu.id());
				for (ItemApuComponent comp : components) {
					if (!comp.priceLocked() && comp.sourceInsumoId() != null) {
						PriceHistory latestPrice = priceHistoryRepository.findByInsumoIdOrderByPriceDateDesc(comp.sourceInsumoId()).stream()
								.filter(ph -> ph.organizationId().equals(orgId) && ph.status() == PriceStatus.VIGENTE)
								.findFirst()
								.orElse(null);

						if (latestPrice != null && latestPrice.price().compareTo(comp.unitPrice()) != 0) {
							comp.changeUnitPrice(latestPrice.price(), "Historial: " + latestPrice.id());
							itemApuComponentRepository.save(comp);
							appliedCount++;
						}
					}
				}
			}
		}

		// Recalculate version to apply changes to totals
		calculationService.calculateVersion(orgId, versionId);

		auditService.log(orgId, userId, "APPLY_PRICES", "BudgetVersion", versionId, null, "Applied new prices to " + appliedCount + " components", "Applied newest active prices", null);
	}

	// Quality and Alerts API
	@Transactional(readOnly = true)
	public QualityReportDto getQualityReport(UUID versionId) {
		BudgetVersion version = getVersion(versionId);
		List<BudgetItem> items = getItems(versionId);

		// Calculate quality score
		int score = alertGenerationService.calculateQualityScore(version, items);

		// Generate check list details
		List<QualityCheckDto> checks = new ArrayList<>();
		UUID orgIdActive = com.backset.forze.shared.TenantContext.getTenantId();
		LocalDate today = LocalDate.now();

		// 1. APU check
		int noApuCount = 0;
		for (BudgetItem item : items) {
			ItemApu apu = itemApuRepository.findByBudgetItemId(item.id()).orElse(null);
			if (apu == null) {
				noApuCount++;
			} else {
				List<ItemApuComponent> components = itemApuComponentRepository.findByItemApuIdOrderByPosition(apu.id());
				if (components.isEmpty()) {
					noApuCount++;
				}
			}
		}
		checks.add(new QualityCheckDto("Rubros con APU completo", noApuCount == 0,
				noApuCount == 0 ? "Todos los rubros tienen APU configurado." : noApuCount + " rubro(s) sin APU o sin componentes.",
				Math.min(noApuCount * 10, 40)));

		// 2. Expired prices check
		int expiredPricesCount = 0;
		for (BudgetItem item : items) {
			ItemApu apu = itemApuRepository.findByBudgetItemId(item.id()).orElse(null);
			if (apu != null) {
				List<ItemApuComponent> components = itemApuComponentRepository.findByItemApuIdOrderByPosition(apu.id());
				for (ItemApuComponent comp : components) {
					if (comp.sourceInsumoId() != null && orgIdActive != null) {
						PriceHistory latestPrice = priceHistoryRepository.findByInsumoIdOrderByPriceDateDesc(comp.sourceInsumoId()).stream()
								.filter(ph -> ph.organizationId().equals(orgIdActive) && ph.status() == PriceStatus.VIGENTE)
								.findFirst()
								.orElse(null);
						if (latestPrice != null && (latestPrice.status() == PriceStatus.VENCIDO || (latestPrice.validUntil() != null && latestPrice.validUntil().isBefore(today)))) {
							expiredPricesCount++;
						}
					}
				}
			}
		}
		checks.add(new QualityCheckDto("Vigencia de precios", expiredPricesCount == 0,
				expiredPricesCount == 0 ? "Todos los precios de insumos vigentes." : expiredPricesCount + " precio(s) vencido(s).",
				Math.min(expiredPricesCount * 5, 20)));

		// 3. Price source check
		int noSourceCount = 0;
		for (BudgetItem item : items) {
			ItemApu apu = itemApuRepository.findByBudgetItemId(item.id()).orElse(null);
			if (apu != null) {
				List<ItemApuComponent> components = itemApuComponentRepository.findByItemApuIdOrderByPosition(apu.id());
				for (ItemApuComponent comp : components) {
					if (comp.priceSource() == null || comp.priceSource().isBlank()) {
						noSourceCount++;
					}
				}
			}
		}
		checks.add(new QualityCheckDto("Fuentes de precios registradas", noSourceCount == 0,
				noSourceCount == 0 ? "Todos los componentes tienen fuente de precio." : noSourceCount + " componente(s) sin fuente registrada.",
				Math.min(noSourceCount * 5, 20)));

		// 4. Measurements check
		int noMeasurementCount = 0;
		for (BudgetItem item : items) {
			if (item.quantity() != null && item.quantity().compareTo(BigDecimal.ZERO) > 0) {
				List<Measurement> measurements = measurementRepository.findByBudgetItemIdOrderByPosition(item.id());
				if (measurements.isEmpty()) {
					noMeasurementCount++;
				} else {
					BigDecimal sum = measurements.stream()
							.map(m -> m.result() != null ? m.result() : BigDecimal.ZERO)
							.reduce(BigDecimal.ZERO, BigDecimal::add);
					if (sum.subtract(item.quantity()).abs().compareTo(new BigDecimal("0.01")) > 0) {
						noMeasurementCount++;
					}
				}
			}
		}
		checks.add(new QualityCheckDto("Mediciones detalladas", noMeasurementCount == 0,
				noMeasurementCount == 0 ? "Todas las cantidades tienen mediciones que coinciden." : noMeasurementCount + " rubro(s) con mediciones faltantes o desalineadas.",
				Math.min(noMeasurementCount * 5, 20)));

		// 5. APU yields check
		int unverifiedYieldCount = 0;
		for (BudgetItem item : items) {
			ItemApu apu = itemApuRepository.findByBudgetItemId(item.id()).orElse(null);
			if (apu != null) {
				if (apu.yield() == null || apu.yield().compareTo(BigDecimal.ZERO) <= 0) {
					unverifiedYieldCount++;
				}
			}
		}
		checks.add(new QualityCheckDto("Rendimientos de APU verificados", unverifiedYieldCount == 0,
				unverifiedYieldCount == 0 ? "Todos los rendimientos son mayores a cero." : unverifiedYieldCount + " rubro(s) con rendimiento cero o nulo.",
				Math.min(unverifiedYieldCount * 5, 20)));

		// 6. Mitigated risks check
		List<BudgetRisk> risks = budgetRiskRepository.findByBudgetVersionId(versionId);
		int unmitigatedRisks = 0;
		for (BudgetRisk risk : risks) {
			if (!risk.mitigated() || risk.mitigation() == null || risk.mitigation().isBlank()) {
				unmitigatedRisks++;
			}
		}
		checks.add(new QualityCheckDto("Riesgos mitigados", unmitigatedRisks == 0,
				unmitigatedRisks == 0 ? "Todos los riesgos registrados están mitigados." : unmitigatedRisks + " riesgo(s) sin mitigar.",
				Math.min(unmitigatedRisks * 10, 30)));

		// Generate budget alerts
		List<AlertGenerationService.BudgetAlert> alerts = alertGenerationService.generateAlerts(version, items, BigDecimal.ZERO);

		return new QualityReportDto(score, checks, alerts);
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

	public record CreateRiskCmd(
			String description,
			BigDecimal probability,
			BigDecimal impact,
			String assignedTo,
			String mitigation,
			boolean mitigated
	) {}

	public record PriceUpdatePreviewDto(
			List<PriceChangeDto> changes,
			BigDecimal currentTotalCost,
			BigDecimal proposedTotalCost,
			BigDecimal difference
	) {}

	public record PriceChangeDto(
			UUID componentId,
			UUID insumoId,
			String insumoCode,
			String insumoName,
			String componentDescription,
			BigDecimal oldPrice,
			BigDecimal newPrice,
			BigDecimal difference,
			BigDecimal currentLineTotal,
			BigDecimal proposedLineTotal,
			BigDecimal lineTotalDifference
	) {}

	public record QualityReportDto(
			int score,
			List<QualityCheckDto> checks,
			List<AlertGenerationService.BudgetAlert> alerts
	) {}

	public record QualityCheckDto(
			String name,
			boolean passed,
			String description,
			int penalty
	) {}
}
