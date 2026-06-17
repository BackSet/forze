package com.backset.forze.module.budgeting.catalog.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.backset.forze.module.budgeting.catalog.application.CatalogService;
import com.backset.forze.module.budgeting.domain.catalog.ApuComponent;
import com.backset.forze.module.budgeting.domain.catalog.ApuMaestro;
import com.backset.forze.module.budgeting.domain.catalog.ComponentSection;
import com.backset.forze.module.budgeting.domain.catalog.Insumo;
import com.backset.forze.module.budgeting.domain.catalog.InsumoType;
import com.backset.forze.module.budgeting.domain.catalog.RubroMaestro;
import com.backset.forze.shared.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.access.prepost.PreAuthorize;
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
public class CatalogController {

	private final CatalogService catalogService;

	public CatalogController(CatalogService catalogService) {
		this.catalogService = catalogService;
	}

	// Insumos
	@GetMapping("/insumos")
	@Operation(summary = "List all insumos in the active organization.")
	@PreAuthorize("@securityService.hasPermission('CATALOGOS_READ')")
	public List<InsumoDto> listInsumos() {
		UUID orgId = TenantContext.getRequiredTenantId();
		return catalogService.getInsumos(orgId).stream()
				.map(this::toDto)
				.toList();
	}

	@GetMapping("/insumos/{id}")
	@Operation(summary = "Get insumo details.")
	@PreAuthorize("@securityService.hasPermission('CATALOGOS_READ')")
	public InsumoDto getInsumo(@PathVariable UUID id) {
		UUID orgId = TenantContext.getRequiredTenantId();
		return toDto(catalogService.getInsumo(orgId, id));
	}

	@PostMapping("/insumos")
	@Operation(summary = "Create a new insumo.")
	@PreAuthorize("@securityService.hasPermission('CATALOGOS_WRITE')")
	public InsumoDto createInsumo(@Valid @RequestBody CreateInsumoRequest request) {
		UUID orgId = TenantContext.getRequiredTenantId();
		Insumo insumo = catalogService.createInsumo(orgId, toCmd(request));
		return toDto(insumo);
	}

	@PutMapping("/insumos/{id}")
	@Operation(summary = "Update an existing insumo.")
	@PreAuthorize("@securityService.hasPermission('CATALOGOS_WRITE')")
	public InsumoDto updateInsumo(@PathVariable UUID id, @Valid @RequestBody CreateInsumoRequest request) {
		UUID orgId = TenantContext.getRequiredTenantId();
		Insumo insumo = catalogService.updateInsumo(orgId, id, toCmd(request));
		return toDto(insumo);
	}

	@DeleteMapping("/insumos/{id}")
	@Operation(summary = "Archive an insumo.")
	@PreAuthorize("@securityService.hasPermission('CATALOGOS_WRITE')")
	public void archiveInsumo(@PathVariable UUID id) {
		UUID orgId = TenantContext.getRequiredTenantId();
		catalogService.archiveInsumo(orgId, id);
	}

	// APU Maestros
	@GetMapping("/apuses")
	@Operation(summary = "List all APUs in the active organization.")
	@PreAuthorize("@securityService.hasPermission('CATALOGOS_READ')")
	public List<ApuMaestroDto> listApuses() {
		UUID orgId = TenantContext.getRequiredTenantId();
		return catalogService.getApuses(orgId).stream()
				.map(this::toDto)
				.toList();
	}

	@GetMapping("/apuses/{id}")
	@Operation(summary = "Get APU details.")
	@PreAuthorize("@securityService.hasPermission('CATALOGOS_READ')")
	public ApuMaestroDto getApu(@PathVariable UUID id) {
		UUID orgId = TenantContext.getRequiredTenantId();
		return toDto(catalogService.getApu(orgId, id));
	}

	@PostMapping("/apuses")
	@Operation(summary = "Create a new APU.")
	@PreAuthorize("@securityService.hasPermission('CATALOGOS_WRITE')")
	public ApuMaestroDto createApu(@Valid @RequestBody CreateApuRequest request) {
		UUID orgId = TenantContext.getRequiredTenantId();
		ApuMaestro apu = catalogService.createApu(orgId, toCmd(request));
		return toDto(apu);
	}

	@PutMapping("/apuses/{id}")
	@Operation(summary = "Update an existing APU.")
	@PreAuthorize("@securityService.hasPermission('CATALOGOS_WRITE')")
	public ApuMaestroDto updateApu(@PathVariable UUID id, @Valid @RequestBody CreateApuRequest request) {
		UUID orgId = TenantContext.getRequiredTenantId();
		ApuMaestro apu = catalogService.updateApu(orgId, id, toCmd(request));
		return toDto(apu);
	}

	@DeleteMapping("/apuses/{id}")
	@Operation(summary = "Archive an APU.")
	@PreAuthorize("@securityService.hasPermission('CATALOGOS_WRITE')")
	public void archiveApu(@PathVariable UUID id) {
		UUID orgId = TenantContext.getRequiredTenantId();
		catalogService.archiveApu(orgId, id);
	}

	// APU Components
	@GetMapping("/apuses/{apuId}/components")
	@Operation(summary = "List all components for a master APU.")
	@PreAuthorize("@securityService.hasPermission('CATALOGOS_READ')")
	public List<ApuComponentDto> listComponents(@PathVariable UUID apuId) {
		UUID orgId = TenantContext.getRequiredTenantId();
		return catalogService.getApuComponents(orgId, apuId).stream()
				.map(this::toDto)
				.toList();
	}

	@PostMapping("/apuses/{apuId}/components")
	@Operation(summary = "Add a component to a master APU.")
	@PreAuthorize("@securityService.hasPermission('CATALOGOS_WRITE')")
	public ApuComponentDto addComponent(@PathVariable UUID apuId, @Valid @RequestBody AddComponentRequest request) {
		UUID orgId = TenantContext.getRequiredTenantId();
		ApuComponent component = catalogService.addComponent(orgId, apuId, toCmd(request));
		return toDto(component);
	}

	@PutMapping("/apuses/{apuId}/components/{componentId}")
	@Operation(summary = "Update a component in a master APU.")
	@PreAuthorize("@securityService.hasPermission('CATALOGOS_WRITE')")
	public ApuComponentDto updateComponent(
			@PathVariable UUID apuId,
			@PathVariable UUID componentId,
			@Valid @RequestBody AddComponentRequest request
	) {
		UUID orgId = TenantContext.getRequiredTenantId();
		ApuComponent component = catalogService.updateComponent(orgId, apuId, componentId, toCmd(request));
		return toDto(component);
	}

	@DeleteMapping("/apuses/{apuId}/components/{componentId}")
	@Operation(summary = "Remove a component from a master APU.")
	@PreAuthorize("@securityService.hasPermission('CATALOGOS_WRITE')")
	public void removeComponent(@PathVariable UUID apuId, @PathVariable UUID componentId) {
		UUID orgId = TenantContext.getRequiredTenantId();
		catalogService.removeComponent(orgId, apuId, componentId);
	}

	// Rubro Maestros
	@GetMapping("/rubros")
	@Operation(summary = "List all rubros in the active organization.")
	@PreAuthorize("@securityService.hasPermission('CATALOGOS_READ')")
	public List<RubroMaestroDto> listRubros() {
		UUID orgId = TenantContext.getRequiredTenantId();
		return catalogService.getRubros(orgId).stream()
				.map(this::toDto)
				.toList();
	}

	@GetMapping("/rubros/{id}")
	@Operation(summary = "Get rubro details.")
	@PreAuthorize("@securityService.hasPermission('CATALOGOS_READ')")
	public RubroMaestroDto getRubro(@PathVariable UUID id) {
		UUID orgId = TenantContext.getRequiredTenantId();
		return toDto(catalogService.getRubro(orgId, id));
	}

	@PostMapping("/rubros")
	@Operation(summary = "Create a new rubro.")
	@PreAuthorize("@securityService.hasPermission('CATALOGOS_WRITE')")
	public RubroMaestroDto createRubro(@Valid @RequestBody CreateRubroRequest request) {
		UUID orgId = TenantContext.getRequiredTenantId();
		RubroMaestro rubro = catalogService.createRubro(orgId, toCmd(request));
		return toDto(rubro);
	}

	@PutMapping("/rubros/{id}")
	@Operation(summary = "Update an existing rubro.")
	@PreAuthorize("@securityService.hasPermission('CATALOGOS_WRITE')")
	public RubroMaestroDto updateRubro(@PathVariable UUID id, @Valid @RequestBody CreateRubroRequest request) {
		UUID orgId = TenantContext.getRequiredTenantId();
		RubroMaestro rubro = catalogService.updateRubro(orgId, id, toCmd(request));
		return toDto(rubro);
	}

	@DeleteMapping("/rubros/{id}")
	@Operation(summary = "Archive a rubro.")
	@PreAuthorize("@securityService.hasPermission('CATALOGOS_WRITE')")
	public void archiveRubro(@PathVariable UUID id) {
		UUID orgId = TenantContext.getRequiredTenantId();
		catalogService.archiveRubro(orgId, id);
	}

	// Helpers & Mappers
	private InsumoDto toDto(Insumo i) {
		return new InsumoDto(
				i.id(), i.organizationId(), i.code(), i.name(), i.description(),
				i.unitId(), i.type().name(), i.categoryId(), i.brand(), i.specification(),
				i.status().name(), i.referencePrice(), i.referencePriceCurrency()
		);
	}

	private CatalogService.CreateInsumoCmd toCmd(CreateInsumoRequest r) {
		return new CatalogService.CreateInsumoCmd(
				r.code(), r.name(), r.description(), r.unitId(),
				InsumoType.valueOf(r.type()), r.categoryId(), r.brand(), r.specification(),
				r.referencePrice(), r.referencePriceCurrency()
		);
	}

	private ApuMaestroDto toDto(ApuMaestro a) {
		return new ApuMaestroDto(
				a.id(), a.organizationId(), a.code(), a.name(), a.unitId(),
				a.versionNumber(), a.status().name(), a.yield(), a.estimatedCost(), a.validUntil()
		);
	}

	private CatalogService.CreateApuCmd toCmd(CreateApuRequest r) {
		return new CatalogService.CreateApuCmd(
				r.code(), r.name(), r.unitId(), r.yield(), r.validUntil()
		);
	}

	private ApuComponentDto toDto(ApuComponent c) {
		return new ApuComponentDto(
				c.id(), c.apuMaestroId(), c.section().name(), c.insumoId(),
				c.description(), c.unitId(), c.quantity(), c.yield(), c.unitPrice(),
				c.wasteFactor(), c.position()
		);
	}

	private CatalogService.AddComponentCmd toCmd(AddComponentRequest r) {
		return new CatalogService.AddComponentCmd(
				ComponentSection.valueOf(r.section()), r.insumoId(), r.description(),
				r.unitId(), r.quantity(), r.yield(), r.wasteFactor(), r.unitPrice()
		);
	}

	private RubroMaestroDto toDto(RubroMaestro r) {
		return new RubroMaestroDto(
				r.id(), r.organizationId(), r.code(), r.name(), r.description(),
				r.categoryId(), r.unitId(), r.specification(), r.status().name(), r.baseApuId()
		);
	}

	private CatalogService.CreateRubroCmd toCmd(CreateRubroRequest r) {
		return new CatalogService.CreateRubroCmd(
				r.code(), r.name(), r.description(), r.unitId(),
				r.categoryId(), r.specification(), r.keywords(), r.baseApuId()
		);
	}

	// Request records
	public record CreateInsumoRequest(
			@NotBlank @Size(min = 2, max = 60) String code,
			@NotBlank @Size(min = 3, max = 200) String name,
			String description,
			@NotNull UUID unitId,
			@NotBlank String type,
			UUID categoryId,
			String brand,
			String specification,
			BigDecimal referencePrice,
			String referencePriceCurrency
	) {}

	public record CreateApuRequest(
			@NotBlank @Size(min = 2, max = 60) String code,
			@NotBlank @Size(min = 3, max = 200) String name,
			@NotNull UUID unitId,
			@NotNull BigDecimal yield,
			LocalDate validUntil
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

	public record CreateRubroRequest(
			@NotBlank @Size(min = 2, max = 60) String code,
			@NotBlank @Size(min = 3, max = 200) String name,
			String description,
			@NotNull UUID unitId,
			UUID categoryId,
			String specification,
			String keywords,
			UUID baseApuId
	) {}

	// DTO records
	public record InsumoDto(
			UUID id, UUID organizationId, String code, String name, String description,
			UUID unitId, String type, UUID categoryId, String brand, String specification,
			String status, BigDecimal referencePrice, String referencePriceCurrency
	) {}

	public record ApuMaestroDto(
			UUID id, UUID organizationId, String code, String name, UUID unitId,
			int versionNumber, String status, BigDecimal yield, BigDecimal estimatedCost, LocalDate validUntil
	) {}

	public record ApuComponentDto(
			UUID id, UUID apuMaestroId, String section, UUID insumoId,
			String description, UUID unitId, BigDecimal quantity, BigDecimal yield, BigDecimal unitPrice,
			BigDecimal wasteFactor, int position
	) {}

	public record RubroMaestroDto(
			UUID id, UUID organizationId, String code, String name, String description,
			UUID categoryId, UUID unitId, String specification, String status, UUID baseApuId
	) {}
}
