package com.backset.forze.module.budgeting.coding.api;

import java.util.UUID;

import com.backset.forze.module.budgeting.coding.application.CodeGenerationService;
import com.backset.forze.shared.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Suggests the next business code for the active organization. Each endpoint
 * requires the same write permission as creating the corresponding entity, so a
 * user without write access receives 403.
 */
@RestController
@RequestMapping("/api")
public class CodeController {

	private final CodeGenerationService codeGenerationService;

	public CodeController(CodeGenerationService codeGenerationService) {
		this.codeGenerationService = codeGenerationService;
	}

	@GetMapping("/projects/next-code")
	@Operation(summary = "Suggest the next project code for the active organization.")
	@PreAuthorize("@securityService.hasPermission('PROYECTOS_WRITE')")
	public NextCodeResponse nextProjectCode() {
		return new NextCodeResponse(codeGenerationService.nextProjectCode(TenantContext.getRequiredTenantId()));
	}

	@GetMapping("/projects/{projectId}/budgets/next-code")
	@Operation(summary = "Suggest the next budget code for a project (codes are unique per project).")
	@PreAuthorize("@securityService.hasPermission('BUDGETS_WRITE')")
	public NextCodeResponse nextBudgetCode(@PathVariable UUID projectId) {
		return new NextCodeResponse(codeGenerationService.nextBudgetCode(TenantContext.getRequiredTenantId(), projectId));
	}

	@GetMapping("/insumos/next-code")
	@Operation(summary = "Suggest the next insumo code for the active organization.")
	@PreAuthorize("@securityService.hasPermission('CATALOGOS_WRITE')")
	public NextCodeResponse nextInsumoCode() {
		return new NextCodeResponse(codeGenerationService.nextInsumoCode(TenantContext.getRequiredTenantId()));
	}

	@GetMapping("/apuses/next-code")
	@Operation(summary = "Suggest the next master APU code for the active organization.")
	@PreAuthorize("@securityService.hasPermission('CATALOGOS_WRITE')")
	public NextCodeResponse nextApuCode() {
		return new NextCodeResponse(codeGenerationService.nextApuCode(TenantContext.getRequiredTenantId()));
	}

	@GetMapping("/rubros/next-code")
	@Operation(summary = "Suggest the next master rubro code for the active organization.")
	@PreAuthorize("@securityService.hasPermission('CATALOGOS_WRITE')")
	public NextCodeResponse nextRubroCode() {
		return new NextCodeResponse(codeGenerationService.nextRubroCode(TenantContext.getRequiredTenantId()));
	}

	public record NextCodeResponse(String code) {}
}
