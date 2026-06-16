package com.backset.forze.module.budgeting.domain.supplier;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Supplier quotation (design section 12.2).
 */
@Entity
@Table(name = "budgeting_quotations")
public class Quotation {

	@Id
	private UUID id;

	@Column(name = "organization_id", nullable = false)
	private UUID organizationId;

	@Column(name = "supplier_id", nullable = false)
	private UUID supplierId;

	@Column(name = "quotation_date", nullable = false)
	private LocalDate quotationDate;

	@Column(name = "valid_until")
	private LocalDate validUntil;

	@Column(name = "currency_code", nullable = false, length = 3)
	private String currencyCode;

	@Column(name = "tax_config_id")
	private UUID taxConfigId;

	@Column(name = "transport_amount", precision = 18, scale = 4)
	private BigDecimal transportAmount;

	@Column(columnDefinition = "text")
	private String conditions;

	@Column(name = "attachment_ref", length = 500)
	private String attachmentRef;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private QuotationStatus status;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	private long version;

	protected Quotation() {
	}

	public Quotation(UUID id, UUID organizationId, UUID supplierId, LocalDate quotationDate, String currencyCode) {
		this.id = id;
		this.organizationId = organizationId;
		this.supplierId = supplierId;
		this.quotationDate = quotationDate;
		this.currencyCode = currencyCode;
		this.status = QuotationStatus.VIGENTE;
	}

	@PrePersist
	void onCreate() {
		Instant now = Instant.now();
		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = Instant.now();
	}

	public void updateTerms(LocalDate validUntil, UUID taxConfigId, BigDecimal transportAmount, String conditions,
			String attachmentRef) {
		this.validUntil = validUntil;
		this.taxConfigId = taxConfigId;
		this.transportAmount = transportAmount;
		this.conditions = conditions;
		this.attachmentRef = attachmentRef;
	}

	public void expire() {
		this.status = QuotationStatus.EXPIRADA;
	}

	public UUID id() {
		return id;
	}

	public UUID organizationId() {
		return organizationId;
	}

	public UUID supplierId() {
		return supplierId;
	}

	public LocalDate quotationDate() {
		return quotationDate;
	}

	public LocalDate validUntil() {
		return validUntil;
	}

	public String currencyCode() {
		return currencyCode;
	}

	public QuotationStatus status() {
		return status;
	}
}
