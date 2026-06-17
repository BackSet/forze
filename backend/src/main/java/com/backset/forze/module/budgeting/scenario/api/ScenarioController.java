package com.backset.forze.module.budgeting.scenario.api;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.backset.forze.module.budgeting.domain.scenario.Scenario;
import com.backset.forze.module.budgeting.domain.scenario.ScenarioType;
import com.backset.forze.module.budgeting.scenario.application.ScenarioService;
import com.backset.forze.shared.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ScenarioController {

	private final ScenarioService scenarioService;

	public ScenarioController(ScenarioService scenarioService) {
		this.scenarioService = scenarioService;
	}

	@GetMapping("/budget-versions/{versionId}/scenarios")
	@Operation(summary = "List all scenarios for a budget version.")
	@PreAuthorize("@securityService.hasPermission('BUDGETS_READ')")
	public List<ScenarioDto> getScenarios(@PathVariable UUID versionId) {
		return scenarioService.getScenarios(versionId).stream()
				.map(this::toDto)
				.toList();
	}

	@PostMapping("/budget-versions/{versionId}/scenarios")
	@Operation(summary = "Create a new scenario for a budget version.")
	@PreAuthorize("@securityService.hasPermission('BUDGETS_WRITE')")
	public ScenarioDto createScenario(@PathVariable UUID versionId, @Valid @RequestBody CreateScenarioRequest request) {
		UUID orgId = TenantContext.getRequiredTenantId();
		List<ScenarioService.OverrideCmd> overrides = request.overrides().stream()
				.map(o -> new ScenarioService.OverrideCmd(o.itemApuComponentId(), o.supplierId(), o.unitPrice(), o.yield(), o.wasteFactor()))
				.toList();

		ScenarioService.CreateScenarioCmd cmd = new ScenarioService.CreateScenarioCmd(
				request.name(),
				ScenarioType.valueOf(request.type()),
				request.utilityRate(),
				request.indirectRate(),
				request.contingencyRate(),
				request.durationDays(),
				request.constructionMethod(),
				overrides
		);

		Scenario scenario = scenarioService.createScenario(orgId, versionId, cmd);
		return toDto(scenario);
	}

	@PutMapping("/scenarios/{id}/primary")
	@Operation(summary = "Select this scenario as the primary/principal budget version option.")
	@PreAuthorize("@securityService.hasPermission('BUDGETS_WRITE')")
	public ScenarioDto makePrimary(@PathVariable UUID id) {
		UUID orgId = TenantContext.getRequiredTenantId();
		Scenario scenario = scenarioService.makeScenarioPrimary(orgId, id);
		return toDto(scenario);
	}

	private ScenarioDto toDto(Scenario s) {
		return new ScenarioDto(
				s.id(),
				s.budgetVersionId(),
				s.name(),
				s.type().name(),
				s.primary(),
				s.utilityRate(),
				s.indirectRate(),
				s.contingencyRate(),
				s.totalCost(),
				s.salePrice(),
				s.margin(),
				s.risk() != null ? s.risk().name() : null
		);
	}

	public record CreateScenarioRequest(
			@NotBlank @Size(min = 3, max = 200) String name,
			@NotBlank String type,
			BigDecimal utilityRate,
			BigDecimal indirectRate,
			BigDecimal contingencyRate,
			Integer durationDays,
			String constructionMethod,
			@NotNull List<OverrideRequest> overrides
	) {}

	public record OverrideRequest(
			@NotNull UUID itemApuComponentId,
			UUID supplierId,
			BigDecimal unitPrice,
			BigDecimal yield,
			BigDecimal wasteFactor
	) {}

	public record ScenarioDto(
			UUID id,
			UUID budgetVersionId,
			String name,
			String type,
			boolean primary,
			BigDecimal utilityRate,
			BigDecimal indirectRate,
			BigDecimal contingencyRate,
			BigDecimal totalCost,
			BigDecimal salePrice,
			BigDecimal margin,
			String risk
	) {}
}
