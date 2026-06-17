package com.backset.forze.module.budgeting.supplier.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.backset.forze.module.budgeting.domain.supplier.PriceHistory;
import com.backset.forze.module.budgeting.domain.supplier.PriceStatus;
import com.backset.forze.module.budgeting.domain.supplier.Quotation;
import com.backset.forze.module.budgeting.domain.supplier.QuotationItem;
import com.backset.forze.module.budgeting.domain.supplier.Supplier;
import com.backset.forze.module.budgeting.infrastructure.PriceHistoryRepository;
import com.backset.forze.module.budgeting.infrastructure.QuotationItemRepository;
import com.backset.forze.module.budgeting.infrastructure.QuotationRepository;
import com.backset.forze.module.budgeting.infrastructure.SupplierRepository;
import com.backset.forze.module.budgeting.audit.application.AuditService;
import com.backset.forze.shared.api.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SupplierService {

	private final SupplierRepository supplierRepository;
	private final QuotationRepository quotationRepository;
	private final QuotationItemRepository quotationItemRepository;
	private final PriceHistoryRepository priceHistoryRepository;
	private final AuditService auditService;

	public SupplierService(
			SupplierRepository supplierRepository,
			QuotationRepository quotationRepository,
			QuotationItemRepository quotationItemRepository,
			PriceHistoryRepository priceHistoryRepository,
			AuditService auditService
	) {
		this.supplierRepository = supplierRepository;
		this.quotationRepository = quotationRepository;
		this.quotationItemRepository = quotationItemRepository;
		this.priceHistoryRepository = priceHistoryRepository;
		this.auditService = auditService;
	}

	@Transactional(readOnly = true)
	public List<Supplier> getSuppliers(UUID orgId) {
		return supplierRepository.findByOrganizationId(orgId);
	}

	@Transactional(readOnly = true)
	public Supplier getSupplier(UUID orgId, UUID id) {
		Supplier supplier = supplierRepository.findById(id)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Proveedor no encontrado."));
		if (!supplier.organizationId().equals(orgId)) {
			throw new ApiException(HttpStatus.FORBIDDEN, "El proveedor no pertenece a la organizacion activa.");
		}
		return supplier;
	}

	@Transactional
	public Supplier createSupplier(UUID orgId, CreateSupplierCmd cmd) {
		Supplier supplier = new Supplier(UUID.randomUUID(), orgId, cmd.legalName());
		supplier.updateContact(cmd.taxId(), cmd.contactName(), cmd.phone(), cmd.email(), cmd.city());
		supplier.updateCommercialTerms(cmd.offeredProducts(), cmd.paymentTerms(), cmd.deliveryTime(), cmd.rating());
		return supplierRepository.save(supplier);
	}

	@Transactional
	public Supplier updateSupplier(UUID orgId, UUID id, CreateSupplierCmd cmd) {
		Supplier supplier = getSupplier(orgId, id);
		Supplier updated = new Supplier(id, orgId, cmd.legalName());
		updated.updateContact(cmd.taxId(), cmd.contactName(), cmd.phone(), cmd.email(), cmd.city());
		updated.updateCommercialTerms(cmd.offeredProducts(), cmd.paymentTerms(), cmd.deliveryTime(), cmd.rating());
		return supplierRepository.save(updated);
	}

	@Transactional
	public void deactivateSupplier(UUID orgId, UUID id) {
		Supplier supplier = getSupplier(orgId, id);
		supplier.deactivate();
		supplierRepository.save(supplier);
	}

	@Transactional(readOnly = true)
	public List<Quotation> getQuotations(UUID orgId) {
		return quotationRepository.findByOrganizationId(orgId);
	}

	@Transactional
	public Quotation createQuotation(UUID orgId, CreateQuotationCmd cmd) {
		Supplier supplier = getSupplier(orgId, cmd.supplierId());

		UUID id = UUID.randomUUID();
		Quotation quotation = new Quotation(id, orgId, cmd.supplierId(), cmd.quotationDate(), cmd.currencyCode());
		quotation.updateTerms(cmd.validUntil(), cmd.taxConfigId(), cmd.transportAmount(), cmd.conditions(), cmd.attachmentRef());
		Quotation saved = quotationRepository.save(quotation);

		int pos = 1;
		for (QuotationItemCmd itemCmd : cmd.items()) {
			QuotationItem item = new QuotationItem(UUID.randomUUID(), id, itemCmd.unitId(), itemCmd.unitPrice(), pos++);
			item.describe(itemCmd.insumoId(), itemCmd.description(), itemCmd.minOrder(), itemCmd.discount());
			quotationItemRepository.save(item);

			PriceHistory hist = new PriceHistory(UUID.randomUUID(), orgId, itemCmd.insumoId(), itemCmd.unitPrice(), cmd.currencyCode(), cmd.quotationDate());
			hist.withOrigin(cmd.supplierId(), id, cmd.city());
			hist.withInclusions(itemCmd.taxesIncluded(), itemCmd.transportIncluded(), cmd.validUntil());
			hist.withConditions(itemCmd.minOrder(), supplier.paymentTerms());
			priceHistoryRepository.save(hist);
		}

		return saved;
	}

	@Transactional(readOnly = true)
	public List<PriceHistory> getPriceHistory(UUID orgId, UUID insumoId) {
		return priceHistoryRepository.findByInsumoIdOrderByPriceDateDesc(insumoId).stream()
				.filter(h -> h.organizationId().equals(orgId))
				.toList();
	}

	@Transactional
	public PriceHistory updatePriceStatus(UUID orgId, UUID priceId, PriceStatus status, UUID userId) {
		PriceHistory price = priceHistoryRepository.findById(priceId)
				.filter(p -> p.organizationId().equals(orgId))
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Price history record not found"));

		PriceStatus oldStatus = price.status();
		price.changeStatus(status);
		PriceHistory saved = priceHistoryRepository.save(price);

		auditService.log(orgId, userId, "UPDATE_PRICE_STATUS", "PriceHistory", priceId, oldStatus.name(), status.name(), "Price status updated", null);
		return saved;
	}

	public record CreateSupplierCmd(
			String legalName,
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

	public record CreateQuotationCmd(
			UUID supplierId,
			LocalDate quotationDate,
			LocalDate validUntil,
			String currencyCode,
			UUID taxConfigId,
			BigDecimal transportAmount,
			String conditions,
			String attachmentRef,
			String city,
			List<QuotationItemCmd> items
	) {}

	public record QuotationItemCmd(
			UUID insumoId,
			String description,
			UUID unitId,
			BigDecimal unitPrice,
			BigDecimal minOrder,
			BigDecimal discount,
			boolean taxesIncluded,
			boolean transportIncluded
	) {}
}
