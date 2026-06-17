package com.backset.forze.module.budgeting.supplier.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.backset.forze.module.budgeting.domain.supplier.PriceHistory;
import com.backset.forze.module.budgeting.domain.supplier.PriceStatus;
import com.backset.forze.module.budgeting.domain.supplier.Quotation;
import com.backset.forze.module.budgeting.domain.supplier.Supplier;
import com.backset.forze.module.budgeting.supplier.application.SupplierService;
import com.backset.forze.module.identity.infrastructure.UserPrincipal;
import com.backset.forze.shared.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class SupplierController {

	private final SupplierService supplierService;

	public SupplierController(SupplierService supplierService) {
		this.supplierService = supplierService;
	}

	// Suppliers
	@GetMapping("/suppliers")
	@Operation(summary = "List all suppliers in the active organization.")
	@PreAuthorize("@securityService.hasPermission('PROVEEDORES_READ')")
	public List<SupplierDto> listSuppliers() {
		UUID orgId = TenantContext.getRequiredTenantId();
		return supplierService.getSuppliers(orgId).stream()
				.map(this::toDto)
				.toList();
	}

	@GetMapping("/suppliers/{id}")
	@Operation(summary = "Get supplier details.")
	@PreAuthorize("@securityService.hasPermission('PROVEEDORES_READ')")
	public SupplierDto getSupplier(@PathVariable UUID id) {
		UUID orgId = TenantContext.getRequiredTenantId();
		return toDto(supplierService.getSupplier(orgId, id));
	}

	@PostMapping("/suppliers")
	@Operation(summary = "Create a new supplier.")
	@PreAuthorize("@securityService.hasPermission('PROVEEDORES_WRITE')")
	public SupplierDto createSupplier(@Valid @RequestBody CreateSupplierRequest request) {
		UUID orgId = TenantContext.getRequiredTenantId();
		Supplier supplier = supplierService.createSupplier(orgId, toCmd(request));
		return toDto(supplier);
	}

	@PutMapping("/suppliers/{id}")
	@Operation(summary = "Update an existing supplier.")
	@PreAuthorize("@securityService.hasPermission('PROVEEDORES_WRITE')")
	public SupplierDto updateSupplier(@PathVariable UUID id, @Valid @RequestBody CreateSupplierRequest request) {
		UUID orgId = TenantContext.getRequiredTenantId();
		Supplier supplier = supplierService.updateSupplier(orgId, id, toCmd(request));
		return toDto(supplier);
	}

	@PutMapping("/suppliers/{id}/deactivate")
	@Operation(summary = "Deactivate a supplier.")
	@PreAuthorize("@securityService.hasPermission('PROVEEDORES_WRITE')")
	public void deactivateSupplier(@PathVariable UUID id) {
		UUID orgId = TenantContext.getRequiredTenantId();
		supplierService.deactivateSupplier(orgId, id);
	}

	// Quotations
	@GetMapping("/quotations")
	@Operation(summary = "List all quotations in the active organization.")
	@PreAuthorize("@securityService.hasPermission('PROVEEDORES_READ')")
	public List<QuotationDto> listQuotations() {
		UUID orgId = TenantContext.getRequiredTenantId();
		return supplierService.getQuotations(orgId).stream()
				.map(q -> new QuotationDto(
						q.id(),
						q.organizationId(),
						q.supplierId(),
						q.quotationDate(),
						q.validUntil(),
						q.currencyCode(),
						q.status().name()
				))
				.toList();
	}

	@PostMapping("/quotations")
	@Operation(summary = "Create a new quotation.")
	@PreAuthorize("@securityService.hasPermission('PROVEEDORES_WRITE')")
	public QuotationDto createQuotation(@Valid @RequestBody CreateQuotationRequest request) {
		UUID orgId = TenantContext.getRequiredTenantId();
		List<SupplierService.QuotationItemCmd> items = request.items().stream()
				.map(item -> new SupplierService.QuotationItemCmd(
						item.insumoId(),
						item.description(),
						item.unitId(),
						item.unitPrice(),
						item.minOrder(),
						item.discount(),
						item.taxesIncluded(),
						item.transportIncluded()
				))
				.toList();

		SupplierService.CreateQuotationCmd cmd = new SupplierService.CreateQuotationCmd(
				request.supplierId(),
				request.quotationDate(),
				request.validUntil(),
				request.currencyCode(),
				request.taxConfigId(),
				request.transportAmount(),
				request.conditions(),
				request.attachmentRef(),
				request.city(),
				items
		);

		Quotation q = supplierService.createQuotation(orgId, cmd);
		return new QuotationDto(
				q.id(),
				q.organizationId(),
				q.supplierId(),
				q.quotationDate(),
				q.validUntil(),
				q.currencyCode(),
				q.status().name()
		);
	}

	@GetMapping("/price-history/{insumoId}")
	@Operation(summary = "Get price history for an insumo.")
	@PreAuthorize("@securityService.hasPermission('PROVEEDORES_READ')")
	public List<PriceHistoryDto> getPriceHistory(@PathVariable UUID insumoId) {
		UUID orgId = TenantContext.getRequiredTenantId();
		return supplierService.getPriceHistory(orgId, insumoId).stream()
				.map(h -> new PriceHistoryDto(
						h.id(),
						h.insumoId(),
						h.price(),
						h.currencyCode(),
						h.priceDate(),
						h.validUntil(),
						h.status().name(),
						h.supplierId(),
						h.quotationId(),
						h.city(),
						h.taxesIncluded(),
						h.transportIncluded(),
						h.minOrder(),
						h.paymentTerms()
				))
				.toList();
	}

	@PutMapping("/price-history/{priceId}/status")
	@Operation(summary = "Update status of a price history record.")
	@PreAuthorize("@securityService.hasPermission('PROVEEDORES_WRITE')")
	public PriceHistoryDto updatePriceStatus(
			@PathVariable UUID priceId,
			@Valid @RequestBody UpdatePriceStatusRequest request,
			@AuthenticationPrincipal UserPrincipal principal
	) {
		UUID orgId = TenantContext.getRequiredTenantId();
		PriceHistory h = supplierService.updatePriceStatus(orgId, priceId, PriceStatus.valueOf(request.status()), principal.id());
		return new PriceHistoryDto(
				h.id(),
				h.insumoId(),
				h.price(),
				h.currencyCode(),
				h.priceDate(),
				h.validUntil(),
				h.status().name(),
				h.supplierId(),
				h.quotationId(),
				h.city(),
				h.taxesIncluded(),
				h.transportIncluded(),
				h.minOrder(),
				h.paymentTerms()
		);
	}

	private SupplierDto toDto(Supplier s) {
		return new SupplierDto(
				s.id(),
				s.organizationId(),
				s.legalName(),
				s.taxId(),
				s.status().name(),
				s.rating()
		);
	}

	private SupplierService.CreateSupplierCmd toCmd(CreateSupplierRequest r) {
		return new SupplierService.CreateSupplierCmd(
				r.legalName(),
				r.taxId(),
				r.contactName(),
				r.phone(),
				r.email(),
				r.city(),
				r.offeredProducts(),
				r.paymentTerms(),
				r.deliveryTime(),
				r.rating()
		);
	}

	public record CreateSupplierRequest(
			@NotBlank @Size(min = 3, max = 200) String legalName,
			String taxId,
			String contactName,
			String phone,
			String email,
			String city,
			String offeredProducts,
			String paymentTerms,
			String deliveryTime,
			BigDecimal rating
	) {}

	public record CreateQuotationRequest(
			@NotNull UUID supplierId,
			@NotNull LocalDate quotationDate,
			LocalDate validUntil,
			@NotBlank @Size(min = 3, max = 3) String currencyCode,
			UUID taxConfigId,
			BigDecimal transportAmount,
			String conditions,
			String attachmentRef,
			String city,
			@NotNull List<CreateQuotationItemRequest> items
	) {}

	public record CreateQuotationItemRequest(
			@NotNull UUID insumoId,
			String description,
			@NotNull UUID unitId,
			@NotNull BigDecimal unitPrice,
			BigDecimal minOrder,
			BigDecimal discount,
			boolean taxesIncluded,
			boolean transportIncluded
	) {}

	public record SupplierDto(
			UUID id,
			UUID organizationId,
			String legalName,
			String taxId,
			String status,
			BigDecimal rating
	) {}

	public record QuotationDto(
			UUID id,
			UUID organizationId,
			UUID supplierId,
			LocalDate quotationDate,
			LocalDate validUntil,
			String currencyCode,
			String status
	) {}

	public record PriceHistoryDto(
			UUID id,
			UUID insumoId,
			BigDecimal price,
			String currencyCode,
			LocalDate priceDate,
			LocalDate validUntil,
			String status,
			UUID supplierId,
			UUID quotationId,
			String city,
			boolean taxesIncluded,
			boolean transportIncluded,
			BigDecimal minOrder,
			String paymentTerms
	) {}

	public record UpdatePriceStatusRequest(
			@NotBlank String status
	) {}
}
