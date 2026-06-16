package com.backset.forze.module.budgeting.domain.supplier;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * Historical price record per insumo (design sections 9.4, 9.5, 11). Append-only.
 */
@Entity
@Table(name = "budgeting_price_history")
public class PriceHistory {

	@Id
	private UUID id;

	@Column(name = "organization_id", nullable = false)
	private UUID organizationId;

	@Column(name = "insumo_id", nullable = false)
	private UUID insumoId;

	@Column(name = "supplier_id")
	private UUID supplierId;

	@Column(name = "quotation_id")
	private UUID quotationId;

	@Column(length = 120)
	private String city;

	@Column(nullable = false, precision = 18, scale = 4)
	private BigDecimal price;

	@Column(name = "currency_code", nullable = false, length = 3)
	private String currencyCode;

	@Column(name = "price_date", nullable = false)
	private LocalDate priceDate;

	@Column(name = "valid_until")
	private LocalDate validUntil;

	@Column(name = "taxes_included", nullable = false)
	private boolean taxesIncluded;

	@Column(name = "transport_included", nullable = false)
	private boolean transportIncluded;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected PriceHistory() {
	}

	public PriceHistory(UUID id, UUID organizationId, UUID insumoId, BigDecimal price, String currencyCode,
			LocalDate priceDate) {
		this.id = id;
		this.organizationId = organizationId;
		this.insumoId = insumoId;
		this.price = price;
		this.currencyCode = currencyCode;
		this.priceDate = priceDate;
	}

	@PrePersist
	void onCreate() {
		createdAt = Instant.now();
	}

	public void withOrigin(UUID supplierId, UUID quotationId, String city) {
		this.supplierId = supplierId;
		this.quotationId = quotationId;
		this.city = city;
	}

	public void withInclusions(boolean taxesIncluded, boolean transportIncluded, LocalDate validUntil) {
		this.taxesIncluded = taxesIncluded;
		this.transportIncluded = transportIncluded;
		this.validUntil = validUntil;
	}

	public UUID id() {
		return id;
	}

	public UUID insumoId() {
		return insumoId;
	}

	public BigDecimal price() {
		return price;
	}

	public String currencyCode() {
		return currencyCode;
	}

	public LocalDate priceDate() {
		return priceDate;
	}

	public LocalDate validUntil() {
		return validUntil;
	}
}
