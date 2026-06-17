package com.backset.forze.module.budgeting.admin.api;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.backset.forze.module.budgeting.admin.application.CatalogConfigService;
import com.backset.forze.shared.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class CatalogConfigController {

	private final CatalogConfigService configService;

	public CatalogConfigController(CatalogConfigService configService) {
		this.configService = configService;
	}

	// Units of Measure
	@GetMapping("/units")
	@Operation(summary = "List all units of measure in the active organization.")
	@PreAuthorize("@securityService.hasPermission('CATALOGOS_READ')")
	public List<UnitDto> listUnits() {
		UUID orgId = TenantContext.getRequiredTenantId();
		return configService.getUnits(orgId).stream()
				.map(u -> new UnitDto(u.id(), u.code(), u.name()))
				.toList();
	}

	@PostMapping("/units")
	@Operation(summary = "Create a new unit of measure.")
	@PreAuthorize("@securityService.hasPermission('CATALOGOS_WRITE')")
	public UnitDto createUnit(@Valid @RequestBody CreateUnitRequest request) {
		UUID orgId = TenantContext.getRequiredTenantId();
		var unit = configService.createUnit(orgId, request.code(), request.name());
		return new UnitDto(unit.id(), unit.code(), unit.name());
	}

	// Categories
	@GetMapping("/categories")
	@Operation(summary = "List all categories in the active organization.")
	@PreAuthorize("@securityService.hasPermission('CATALOGOS_READ')")
	public List<CategoryDto> listCategories() {
		UUID orgId = TenantContext.getRequiredTenantId();
		return configService.getCategories(orgId).stream()
				.map(c -> new CategoryDto(c.id(), c.code(), c.name()))
				.toList();
	}

	@PostMapping("/categories")
	@Operation(summary = "Create a new category.")
	@PreAuthorize("@securityService.hasPermission('CATALOGOS_WRITE')")
	public CategoryDto createCategory(@Valid @RequestBody CreateCategoryRequest request) {
		UUID orgId = TenantContext.getRequiredTenantId();
		var category = configService.createCategory(orgId, request.code(), request.name());
		return new CategoryDto(category.id(), category.code(), category.name());
	}

	// Taxes
	@GetMapping("/taxes")
	@Operation(summary = "List all tax configurations in the active organization.")
	@PreAuthorize("@securityService.hasPermission('CATALOGOS_READ')")
	public List<TaxDto> listTaxes() {
		UUID orgId = TenantContext.getRequiredTenantId();
		return configService.getTaxes(orgId).stream()
				.map(t -> new TaxDto(t.id(), t.code(), t.name(), t.rate()))
				.toList();
	}

	@PostMapping("/taxes")
	@Operation(summary = "Create a new tax configuration.")
	@PreAuthorize("@securityService.hasPermission('CATALOGOS_WRITE')")
	public TaxDto createTax(@Valid @RequestBody CreateTaxRequest request) {
		UUID orgId = TenantContext.getRequiredTenantId();
		var tax = configService.createTax(orgId, request.code(), request.name(), request.rate());
		return new TaxDto(tax.id(), tax.code(), tax.name(), tax.rate());
	}

	public record CreateUnitRequest(
			@NotBlank @Size(max = 40) String code,
			@NotBlank @Size(max = 120) String name
	) {}

	public record UnitDto(UUID id, String code, String name) {}

	public record CreateCategoryRequest(
			@NotBlank @Size(max = 40) String code,
			@NotBlank @Size(max = 160) String name
	) {}

	public record CategoryDto(UUID id, String code, String name) {}

	public record CreateTaxRequest(
			@NotBlank @Size(max = 40) String code,
			@NotBlank @Size(max = 120) String name,
			@NotNull BigDecimal rate
	) {}

	public record TaxDto(UUID id, String code, String name, BigDecimal rate) {}
}
