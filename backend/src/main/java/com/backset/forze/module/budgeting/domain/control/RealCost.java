package com.backset.forze.module.budgeting.domain.control;

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
import jakarta.persistence.Table;

/**
 * Real cost recorded against a baseline (design section 18.3).
 */
@Entity
@Table(name = "budgeting_real_costs")
public class RealCost {

	@Id
	private UUID id;

	@Column(name = "baseline_id", nullable = false)
	private UUID baselineId;

	@Column(name = "budget_item_id")
	private UUID budgetItemId;

	@Enumerated(EnumType.STRING)
	@Column(name = "cost_type", nullable = false, length = 20)
	private RealCostType costType;

	@Column(nullable = false, precision = 18, scale = 2)
	private BigDecimal amount;

	@Column(name = "currency_code", nullable = false, length = 3)
	private String currencyCode;

	@Column(name = "cost_date", nullable = false)
	private LocalDate costDate;

	@Column(length = 300)
	private String description;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected RealCost() {
	}

	public RealCost(UUID id, UUID baselineId, RealCostType costType, BigDecimal amount, String currencyCode,
			LocalDate costDate) {
		this.id = id;
		this.baselineId = baselineId;
		this.costType = costType;
		this.amount = amount;
		this.currencyCode = currencyCode;
		this.costDate = costDate;
	}

	@PrePersist
	void onCreate() {
		createdAt = Instant.now();
	}

	public void attachToItem(UUID budgetItemId, String description) {
		this.budgetItemId = budgetItemId;
		this.description = description;
	}

	public UUID id() {
		return id;
	}

	public UUID baselineId() {
		return baselineId;
	}

	public RealCostType costType() {
		return costType;
	}

	public BigDecimal amount() {
		return amount;
	}

	public String currencyCode() {
		return currencyCode;
	}

	public LocalDate costDate() {
		return costDate;
	}
}
