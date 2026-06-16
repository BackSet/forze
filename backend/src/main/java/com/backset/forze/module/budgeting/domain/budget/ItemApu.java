package com.backset.forze.module.budgeting.domain.budget;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * APU snapshot attached to a budget item (design section 9.2). One APU per item.
 * source_apu_id keeps the catalog link; the components hold frozen prices.
 */
@Entity
@Table(name = "budgeting_item_apu")
public class ItemApu {

	@Id
	private UUID id;

	@Column(name = "budget_item_id", nullable = false)
	private UUID budgetItemId;

	@Column(name = "source_apu_id")
	private UUID sourceApuId;

	@Column(precision = 18, scale = 6)
	private BigDecimal yield;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected ItemApu() {
	}

	public ItemApu(UUID id, UUID budgetItemId, UUID sourceApuId, BigDecimal yield) {
		this.id = id;
		this.budgetItemId = budgetItemId;
		this.sourceApuId = sourceApuId;
		this.yield = yield;
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

	public void changeYield(BigDecimal yield) {
		this.yield = yield;
	}

	public UUID id() {
		return id;
	}

	public UUID budgetItemId() {
		return budgetItemId;
	}

	public UUID sourceApuId() {
		return sourceApuId;
	}

	public BigDecimal yield() {
		return yield;
	}
}
