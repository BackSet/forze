package com.backset.forze.module.budgeting.domain.supplier;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * Quoted product line (design sections 12.2 and 12.3). Child of the {@link Quotation} aggregate.
 */
@Entity
@Table(name = "budgeting_quotation_items")
public class QuotationItem {

	@Id
	private UUID id;

	@Column(name = "quotation_id", nullable = false)
	private UUID quotationId;

	@Column(name = "insumo_id")
	private UUID insumoId;

	@Column(length = 200)
	private String description;

	@Column(name = "unit_id", nullable = false)
	private UUID unitId;

	@Column(name = "unit_price", nullable = false, precision = 18, scale = 4)
	private BigDecimal unitPrice;

	@Column(name = "min_order", precision = 18, scale = 4)
	private BigDecimal minOrder;

	@Column(precision = 7, scale = 4)
	private BigDecimal discount;

	@Column(nullable = false)
	private int position;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected QuotationItem() {
	}

	public QuotationItem(UUID id, UUID quotationId, UUID unitId, BigDecimal unitPrice, int position) {
		this.id = id;
		this.quotationId = quotationId;
		this.unitId = unitId;
		this.unitPrice = unitPrice;
		this.position = position;
	}

	@PrePersist
	void onCreate() {
		createdAt = Instant.now();
	}

	public void describe(UUID insumoId, String description, BigDecimal minOrder, BigDecimal discount) {
		this.insumoId = insumoId;
		this.description = description;
		this.minOrder = minOrder;
		this.discount = discount;
	}

	public UUID id() {
		return id;
	}

	public UUID quotationId() {
		return quotationId;
	}

	public UUID insumoId() {
		return insumoId;
	}

	public BigDecimal unitPrice() {
		return unitPrice;
	}

	public int position() {
		return position;
	}
}
