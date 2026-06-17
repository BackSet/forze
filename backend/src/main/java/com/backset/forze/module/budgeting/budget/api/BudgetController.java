package com.backset.forze.module.budgeting.budget.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.backset.forze.module.budgeting.budget.application.BudgetService;
import com.backset.forze.module.budgeting.budget.application.BudgetVersionCalculationService;
import com.backset.forze.module.budgeting.domain.budget.Budget;
import com.backset.forze.module.budgeting.domain.budget.BudgetItem;
import com.backset.forze.module.budgeting.domain.budget.BudgetVersion;
import com.backset.forze.module.budgeting.domain.budget.Chapter;
import com.backset.forze.module.budgeting.domain.budget.ItemApu;
import com.backset.forze.module.budgeting.domain.budget.ItemApuComponent;
import com.backset.forze.module.budgeting.domain.budget.Measurement;
import com.backset.forze.module.budgeting.domain.catalog.ComponentSection;
import com.backset.forze.module.budgeting.domain.budget.BudgetRisk;
import com.backset.forze.module.identity.infrastructure.UserPrincipal;
import com.backset.forze.shared.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class BudgetController {

	private final BudgetService budgetService;
	private final BudgetVersionCalculationService calculationService;

	public BudgetController(BudgetService budgetService, BudgetVersionCalculationService calculationService) {
		this.budgetService = budgetService;
		this.calculationService = calculationService;
	}

	// Budgets
	@GetMapping("/projects/{projectId}/budgets")
	@Operation(summary = "List all budgets for a project.")
	@PreAuthorize("@securityService.hasPermission('BUDGETS_READ')")
	public List<BudgetDto> getBudgets(@PathVariable UUID projectId) {
		return budgetService.getBudgetsForProject(projectId).stream()
				.map(b -> new BudgetDto(b.id(), b.organizationId(), b.projectId(), b.code(), b.name(), b.currencyCode()))
				.toList();
	}

	@PostMapping("/projects/{projectId}/budgets")
	@Operation(summary = "Create a new budget for a project.")
	@PreAuthorize("@securityService.hasPermission('BUDGETS_WRITE')")
	public BudgetDto createBudget(
			@PathVariable UUID projectId,
			@Valid @RequestBody CreateBudgetRequest request,
			@AuthenticationPrincipal UserPrincipal principal
	) {
		UUID orgId = TenantContext.getRequiredTenantId();
		Budget b = budgetService.createBudget(orgId, projectId, new BudgetService.CreateBudgetCmd(request.code(), request.name(), request.currencyCode(), principal.id()));
		return new BudgetDto(b.id(), b.organizationId(), b.projectId(), b.code(), b.name(), b.currencyCode());
	}

	// Budget Versions
	@GetMapping("/budgets/{budgetId}/versions")
	@Operation(summary = "List all versions for a budget.")
	@PreAuthorize("@securityService.hasPermission('BUDGETS_READ')")
	public List<VersionDto> getVersions(@PathVariable UUID budgetId) {
		return budgetService.getVersionsForBudget(budgetId).stream()
				.map(this::toDto)
				.toList();
	}

	@GetMapping("/budget-versions/{id}")
	@Operation(summary = "Get budget version details.")
	@PreAuthorize("@securityService.hasPermission('BUDGETS_READ')")
	public VersionDto getVersion(@PathVariable UUID id) {
		return toDto(budgetService.getVersion(id));
	}

	@PostMapping("/budgets/{budgetId}/versions")
	@Operation(summary = "Create a new budget version copying an existing version.")
	@PreAuthorize("@securityService.hasPermission('BUDGETS_WRITE')")
	public VersionDto createVersion(
			@PathVariable UUID budgetId,
			@Valid @RequestBody CreateVersionRequest request,
			@AuthenticationPrincipal UserPrincipal principal
	) {
		UUID orgId = TenantContext.getRequiredTenantId();
		BudgetVersion v = budgetService.copyVersion(orgId, budgetId, request.baseVersionId(), request.name(), request.changeReason(), principal.id());
		return toDto(v);
	}

	@PutMapping("/budget-versions/{id}/financials")
	@Operation(summary = "Configure financial rates and objectives for a version.")
	@PreAuthorize("@securityService.hasPermission('BUDGETS_WRITE')")
	public VersionDto configureFinancials(@PathVariable UUID id, @Valid @RequestBody ConfigureFinancialsRequest request) {
		UUID orgId = TenantContext.getRequiredTenantId();
		BudgetVersion v = budgetService.configureFinancials(orgId, id, new BudgetService.ConfigureFinancialsCmd(
				request.targetAmount(), request.utilityRate(), request.indirectRate(), request.contingencyRate(), request.taxConfigId(), request.validUntil()));
		return toDto(v);
	}

	@PostMapping("/budget-versions/{id}/calculate")
	@Operation(summary = "Trigger cost and price calculation for a budget version.")
	@PreAuthorize("@securityService.hasPermission('BUDGETS_WRITE')")
	public VersionDto calculateVersion(@PathVariable UUID id) {
		UUID orgId = TenantContext.getRequiredTenantId();
		return toDto(calculationService.calculateVersion(orgId, id));
	}

	// Chapters
	@GetMapping("/budget-versions/{versionId}/chapters")
	@Operation(summary = "List all chapters for a budget version.")
	@PreAuthorize("@securityService.hasPermission('BUDGETS_READ')")
	public List<ChapterDto> getChapters(@PathVariable UUID versionId) {
		return budgetService.getChapters(versionId).stream()
				.map(c -> new ChapterDto(c.id(), c.budgetVersionId(), c.parentChapterId(), c.code(), c.name(), c.position()))
				.toList();
	}

	@PostMapping("/budget-versions/{versionId}/chapters")
	@Operation(summary = "Create a new chapter inside a budget version.")
	@PreAuthorize("@securityService.hasPermission('BUDGETS_WRITE')")
	public ChapterDto createChapter(@PathVariable UUID versionId, @Valid @RequestBody CreateChapterRequest request) {
		Chapter c = budgetService.createChapter(versionId, request.name(), request.parentId());
		return new ChapterDto(c.id(), c.budgetVersionId(), c.parentChapterId(), c.code(), c.name(), c.position());
	}

	// Items
	@GetMapping("/budget-versions/{versionId}/items")
	@Operation(summary = "List all items (rubros) in a budget version.")
	@PreAuthorize("@securityService.hasPermission('BUDGETS_READ')")
	public List<ItemDto> getItems(@PathVariable UUID versionId) {
		return budgetService.getItems(versionId).stream()
				.map(this::toDto)
				.toList();
	}

	@PostMapping("/budget-versions/{versionId}/items")
	@Operation(summary = "Add a master rubro as an item to a budget version (taking snapshots).")
	@PreAuthorize("@securityService.hasPermission('BUDGETS_WRITE')")
	public ItemDto addItem(@PathVariable UUID versionId, @Valid @RequestBody AddItemRequest request) {
		UUID orgId = TenantContext.getRequiredTenantId();
		BudgetItem item = budgetService.addRubroToVersion(orgId, versionId, request.rubroId(), request.chapterId(), request.quantity());
		return toDto(item);
	}

	@PutMapping("/budget-items/{id}")
	@Operation(summary = "Update item quantity.")
	@PreAuthorize("@securityService.hasPermission('BUDGETS_WRITE')")
	public ItemDto updateItem(@PathVariable UUID id, @Valid @RequestBody UpdateItemRequest request) {
		UUID orgId = TenantContext.getRequiredTenantId();
		BudgetItem item = budgetService.updateItemQuantity(orgId, id, request.quantity());
		return toDto(item);
	}

	@DeleteMapping("/budget-items/{id}")
	@Operation(summary = "Remove an item from a budget version.")
	@PreAuthorize("@securityService.hasPermission('BUDGETS_WRITE')")
	public void deleteItem(@PathVariable UUID id) {
		UUID orgId = TenantContext.getRequiredTenantId();
		budgetService.deleteItem(orgId, id);
	}

	// APU
	@GetMapping("/budget-items/{id}/apu")
	@Operation(summary = "Get APU snapshot of a budget item.")
	@PreAuthorize("@securityService.hasPermission('BUDGETS_READ')")
	public ItemApuDto getItemApu(@PathVariable UUID id) {
		ItemApu apu = budgetService.getItemApu(id);
		return apu == null ? null : new ItemApuDto(apu.id(), apu.budgetItemId(), apu.sourceApuId(), apu.yield());
	}

	@PutMapping("/budget-items/{id}/apu")
	@Operation(summary = "Update APU yield of a budget item.")
	@PreAuthorize("@securityService.hasPermission('BUDGETS_WRITE')")
	public ItemApuDto updateItemApuYield(@PathVariable UUID id, @Valid @RequestBody UpdateApuYieldRequest request) {
		UUID orgId = TenantContext.getRequiredTenantId();
		ItemApu apu = budgetService.getItemApu(id);
		if (apu == null) {
			return null;
		}
		ItemApu updated = budgetService.updateItemApuYield(orgId, apu.id(), request.yield());
		return new ItemApuDto(updated.id(), updated.budgetItemId(), updated.sourceApuId(), updated.yield());
	}

	// APU Components
	@GetMapping("/item-apus/{apuId}/components")
	@Operation(summary = "List all components of an item's APU.")
	@PreAuthorize("@securityService.hasPermission('BUDGETS_READ')")
	public List<ItemApuComponentDto> getApuComponents(@PathVariable UUID apuId) {
		return budgetService.getItemApuComponents(apuId).stream()
				.map(c -> new ItemApuComponentDto(c.id(), c.itemApuId(), c.section().name(), c.sourceInsumoId(), c.description(), c.unitId(), c.quantity(), c.yield(), c.unitPrice(), c.wasteFactor(), c.lineTotal(), c.position()))
				.toList();
	}

	@PostMapping("/item-apus/{apuId}/components")
	@Operation(summary = "Add a component to an item's APU.")
	@PreAuthorize("@securityService.hasPermission('BUDGETS_WRITE')")
	public ItemApuComponentDto addApuComponent(@PathVariable UUID apuId, @Valid @RequestBody AddComponentRequest request) {
		UUID orgId = TenantContext.getRequiredTenantId();
		ItemApuComponent c = budgetService.addComponentToItemApu(orgId, apuId, toCmd(request));
		return new ItemApuComponentDto(c.id(), c.itemApuId(), c.section().name(), c.sourceInsumoId(), c.description(), c.unitId(), c.quantity(), c.yield(), c.unitPrice(), c.wasteFactor(), c.lineTotal(), c.position());
	}

	@PutMapping("/item-apu-components/{id}")
	@Operation(summary = "Update an APU component.")
	@PreAuthorize("@securityService.hasPermission('BUDGETS_WRITE')")
	public ItemApuComponentDto updateApuComponent(@PathVariable UUID id, @Valid @RequestBody AddComponentRequest request) {
		UUID orgId = TenantContext.getRequiredTenantId();
		ItemApuComponent c = budgetService.updateItemApuComponent(orgId, id, toCmd(request));
		return new ItemApuComponentDto(c.id(), c.itemApuId(), c.section().name(), c.sourceInsumoId(), c.description(), c.unitId(), c.quantity(), c.yield(), c.unitPrice(), c.wasteFactor(), c.lineTotal(), c.position());
	}

	@DeleteMapping("/item-apu-components/{id}")
	@Operation(summary = "Remove a component from an item's APU.")
	@PreAuthorize("@securityService.hasPermission('BUDGETS_WRITE')")
	public void removeApuComponent(@PathVariable UUID id) {
		UUID orgId = TenantContext.getRequiredTenantId();
		budgetService.removeItemApuComponent(orgId, id);
	}

	// Measurements
	@GetMapping("/budget-items/{id}/measurements")
	@Operation(summary = "List measurements for a budget item.")
	@PreAuthorize("@securityService.hasPermission('BUDGETS_READ')")
	public List<MeasurementDto> getMeasurements(@PathVariable UUID id) {
		return budgetService.getItemMeasurements(id).stream()
				.map(m -> new MeasurementDto(m.id(), m.budgetItemId(), m.description(), m.length(), m.width(), m.height(), m.itemCount(), m.factor(), m.formula(), m.result(), m.notes(), m.position()))
				.toList();
	}

	@PostMapping("/budget-items/{id}/measurements")
	@Operation(summary = "Add a measurement line to a budget item.")
	@PreAuthorize("@securityService.hasPermission('BUDGETS_WRITE')")
	public MeasurementDto addMeasurement(@PathVariable UUID id, @Valid @RequestBody AddMeasurementRequest request) {
		UUID orgId = TenantContext.getRequiredTenantId();
		Measurement m = budgetService.addMeasurement(orgId, id, new BudgetService.AddMeasurementCmd(
				request.description(), request.length(), request.width(), request.height(), request.itemCount(), request.factor(), request.formula(), request.notes()));
		return new MeasurementDto(m.id(), m.budgetItemId(), m.description(), m.length(), m.width(), m.height(), m.itemCount(), m.factor(), m.formula(), m.result(), m.notes(), m.position());
	}

	@DeleteMapping("/measurements/{id}")
	@Operation(summary = "Delete a measurement line.")
	@PreAuthorize("@securityService.hasPermission('BUDGETS_WRITE')")
	public void deleteMeasurement(@PathVariable UUID id) {
		UUID orgId = TenantContext.getRequiredTenantId();
		budgetService.deleteMeasurement(orgId, id);
	}

	private VersionDto toDto(BudgetVersion v) {
		return new VersionDto(
				v.id(),
				v.budgetId(),
				v.versionNumber(),
				v.name(),
				v.status().name(),
				v.targetAmount(),
				v.utilityRate(),
				v.indirectRate(),
				v.contingencyRate(),
				v.totalCost(),
				v.salePrice(),
				v.margin(),
				v.viabilityStatus() != null ? v.viabilityStatus().name() : null
		);
	}

	private ItemDto toDto(BudgetItem i) {
		return new ItemDto(
				i.id(),
				i.budgetVersionId(),
				i.chapterId(),
				i.sourceRubroId(),
				i.code(),
				i.name(),
				i.unitId(),
				i.quantity(),
				i.unitCost(),
				i.unitPrice(),
				i.totalCost(),
				i.totalSale(),
				i.margin(),
				i.priceLocked()
		);
	}

	private BudgetService.AddComponentCmd toCmd(AddComponentRequest request) {
		return new BudgetService.AddComponentCmd(
				ComponentSection.valueOf(request.section()),
				request.insumoId(),
				request.description(),
				request.unitId(),
				request.quantity(),
				request.yield(),
				request.wasteFactor(),
				request.unitPrice()
		);
	}

	// Risks
	@GetMapping("/budget-versions/{versionId}/risks")
	@Operation(summary = "List all risks for a budget version.")
	@PreAuthorize("@securityService.hasPermission('PRESUPUESTOS_READ')")
	public List<RiskDto> getRisks(@PathVariable UUID versionId) {
		return budgetService.getRisks(versionId).stream()
				.map(this::toDto)
				.toList();
	}

	@GetMapping("/budget-risks/{id}")
	@Operation(summary = "Get budget risk details.")
	@PreAuthorize("@securityService.hasPermission('PRESUPUESTOS_READ')")
	public RiskDto getRisk(@PathVariable UUID id) {
		UUID orgId = TenantContext.getRequiredTenantId();
		return toDto(budgetService.getRisk(orgId, id));
	}

	@PostMapping("/budget-versions/{versionId}/risks")
	@Operation(summary = "Add a new risk to a budget version.")
	@PreAuthorize("@securityService.hasPermission('PRESUPUESTOS_WRITE')")
	public RiskDto addRisk(
			@PathVariable UUID versionId,
			@Valid @RequestBody CreateRiskRequest request,
			@AuthenticationPrincipal UserPrincipal principal
	) {
		UUID orgId = TenantContext.getRequiredTenantId();
		BudgetRisk risk = budgetService.addRisk(orgId, versionId, new BudgetService.CreateRiskCmd(
				request.description(), request.probability(), request.impact(), request.assignedTo(), request.mitigation(), request.mitigated()), principal.id());
		return toDto(risk);
	}

	@PutMapping("/budget-risks/{id}")
	@Operation(summary = "Update an existing budget risk.")
	@PreAuthorize("@securityService.hasPermission('PRESUPUESTOS_WRITE')")
	public RiskDto updateRisk(
			@PathVariable UUID id,
			@Valid @RequestBody CreateRiskRequest request,
			@AuthenticationPrincipal UserPrincipal principal
	) {
		UUID orgId = TenantContext.getRequiredTenantId();
		BudgetRisk risk = budgetService.updateRisk(orgId, id, new BudgetService.CreateRiskCmd(
				request.description(), request.probability(), request.impact(), request.assignedTo(), request.mitigation(), request.mitigated()), principal.id());
		return toDto(risk);
	}

	@DeleteMapping("/budget-risks/{id}")
	@Operation(summary = "Delete a budget risk.")
	@PreAuthorize("@securityService.hasPermission('PRESUPUESTOS_WRITE')")
	public void deleteRisk(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
		UUID orgId = TenantContext.getRequiredTenantId();
		budgetService.deleteRisk(orgId, id, principal.id());
	}

	// Price Update / Preview
	@GetMapping("/budget-versions/{versionId}/price-update-preview")
	@Operation(summary = "Get preview of applying new prices to budget version components.")
	@PreAuthorize("@securityService.hasPermission('PRESUPUESTOS_READ')")
	public PriceUpdatePreviewResponse getPriceUpdatePreview(@PathVariable UUID versionId) {
		UUID orgId = TenantContext.getRequiredTenantId();
		BudgetService.PriceUpdatePreviewDto preview = budgetService.getPriceUpdatePreview(orgId, versionId);
		return toDto(preview);
	}

	@PostMapping("/budget-versions/{versionId}/apply-new-prices")
	@Operation(summary = "Apply new prices to budget version components.")
	@PreAuthorize("@securityService.hasPermission('PRESUPUESTOS_WRITE')")
	public void applyNewPrices(@PathVariable UUID versionId, @AuthenticationPrincipal UserPrincipal principal) {
		UUID orgId = TenantContext.getRequiredTenantId();
		budgetService.applyNewPrices(orgId, versionId, principal.id());
	}

	// Quality Evaluation
	@GetMapping("/budget-versions/{versionId}/quality")
	@Operation(summary = "Get quality evaluation report for a budget version.")
	@PreAuthorize("@securityService.hasPermission('PRESUPUESTOS_READ')")
	public QualityReportResponse getQualityReport(@PathVariable UUID versionId) {
		BudgetService.QualityReportDto report = budgetService.getQualityReport(versionId);
		return toDto(report);
	}

	private RiskDto toDto(BudgetRisk r) {
		return new RiskDto(
				r.id(),
				r.organizationId(),
				r.budgetVersionId(),
				r.description(),
				r.probability(),
				r.impact(),
				r.expectedAmount(),
				r.assignedTo(),
				r.mitigation(),
				r.mitigated()
		);
	}

	private PriceUpdatePreviewResponse toDto(BudgetService.PriceUpdatePreviewDto p) {
		List<PriceChangeDto> changes = p.changes().stream()
				.map(c -> new PriceChangeDto(
						c.componentId(),
						c.insumoId(),
						c.insumoCode(),
						c.insumoName(),
						c.componentDescription(),
						c.oldPrice(),
						c.newPrice(),
						c.difference(),
						c.currentLineTotal(),
						c.proposedLineTotal(),
						c.lineTotalDifference()
				))
				.toList();
		return new PriceUpdatePreviewResponse(changes, p.currentTotalCost(), p.proposedTotalCost(), p.difference());
	}

	private QualityReportResponse toDto(BudgetService.QualityReportDto r) {
		List<QualityCheckDto> checks = r.checks().stream()
				.map(c -> new QualityCheckDto(c.name(), c.passed(), c.description(), c.penalty()))
				.toList();
		List<BudgetAlertDto> alerts = r.alerts().stream()
				.map(a -> new BudgetAlertDto(a.field(), a.message()))
				.toList();
		return new QualityReportResponse(r.score(), checks, alerts);
	}

	public record CreateBudgetRequest(
			@NotBlank @Size(min = 2, max = 60) String code,
			@NotBlank @Size(min = 3, max = 200) String name,
			@NotBlank @Size(min = 3, max = 3) String currencyCode
	) {}

	public record CreateVersionRequest(
			@NotNull UUID baseVersionId,
			@NotBlank @Size(min = 3, max = 200) String name,
			String changeReason
	) {}

	public record ConfigureFinancialsRequest(
			BigDecimal targetAmount,
			BigDecimal utilityRate,
			BigDecimal indirectRate,
			BigDecimal contingencyRate,
			UUID taxConfigId,
			LocalDate validUntil
	) {}

	public record CreateChapterRequest(
			@NotBlank @Size(min = 3, max = 200) String name,
			UUID parentId
	) {}

	public record AddItemRequest(
			@NotNull UUID rubroId,
			UUID chapterId,
			@NotNull BigDecimal quantity
	) {}

	public record UpdateItemRequest(
			@NotNull BigDecimal quantity
	) {}

	public record UpdateApuYieldRequest(
			@NotNull BigDecimal yield
	) {}

	public record AddComponentRequest(
			@NotBlank String section,
			UUID insumoId,
			String description,
			@NotNull UUID unitId,
			@NotNull BigDecimal quantity,
			BigDecimal yield,
			BigDecimal wasteFactor,
			BigDecimal unitPrice
	) {}

	public record AddMeasurementRequest(
			String description,
			BigDecimal length,
			BigDecimal width,
			BigDecimal height,
			BigDecimal itemCount,
			BigDecimal factor,
			String formula,
			String notes
	) {}

	public record BudgetDto(UUID id, UUID organizationId, UUID projectId, String code, String name, String currencyCode) {}

	public record VersionDto(
			UUID id,
			UUID budgetId,
			int versionNumber,
			String name,
			String status,
			BigDecimal targetAmount,
			BigDecimal utilityRate,
			BigDecimal indirectRate,
			BigDecimal contingencyRate,
			BigDecimal totalCost,
			BigDecimal salePrice,
			BigDecimal margin,
			String viabilityStatus
	) {}

	public record ChapterDto(UUID id, UUID budgetVersionId, UUID parentChapterId, String code, String name, int position) {}

	public record ItemDto(
			UUID id,
			UUID budgetVersionId,
			UUID chapterId,
			UUID sourceRubroId,
			String code,
			String name,
			UUID unitId,
			BigDecimal quantity,
			BigDecimal unitCost,
			BigDecimal unitPrice,
			BigDecimal totalCost,
			BigDecimal totalSale,
			BigDecimal margin,
			boolean priceLocked
	) {}

	public record ItemApuDto(UUID id, UUID budgetItemId, UUID sourceApuId, BigDecimal yield) {}

	public record ItemApuComponentDto(
			UUID id,
			UUID itemApuId,
			String section,
			UUID sourceInsumoId,
			String description,
			UUID unitId,
			BigDecimal quantity,
			BigDecimal yield,
			BigDecimal unitPrice,
			BigDecimal wasteFactor,
			BigDecimal lineTotal,
			int position
	) {}

	public record MeasurementDto(
			UUID id,
			UUID budgetItemId,
			String description,
			BigDecimal length,
			BigDecimal width,
			BigDecimal height,
			BigDecimal itemCount,
			BigDecimal factor,
			String formula,
			BigDecimal result,
			String notes,
			int position
	) {}

	public record CreateRiskRequest(
			@NotBlank String description,
			@NotNull BigDecimal probability,
			@NotNull BigDecimal impact,
			String assignedTo,
			String mitigation,
			boolean mitigated
	) {}

	public record RiskDto(
			UUID id,
			UUID organizationId,
			UUID budgetVersionId,
			String description,
			BigDecimal probability,
			BigDecimal impact,
			BigDecimal expectedAmount,
			String assignedTo,
			String mitigation,
			boolean mitigated
	) {}

	public record PriceUpdatePreviewResponse(
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

	public record QualityReportResponse(
			int score,
			List<QualityCheckDto> checks,
			List<BudgetAlertDto> alerts
	) {}

	public record QualityCheckDto(
			String name,
			boolean passed,
			String description,
			int penalty
	) {}

	public record BudgetAlertDto(
			String field,
			String message
	) {}
}
