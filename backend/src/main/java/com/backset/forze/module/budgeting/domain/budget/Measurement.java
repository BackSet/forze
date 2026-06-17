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
 * Measurement line that builds an item quantity from a formula (design section 9.3).
 * Child of the {@link BudgetItem} aggregate.
 */
@Entity
@Table(name = "budgeting_measurements")
public class Measurement {

	@Id
	private UUID id;

	@Column(name = "budget_item_id", nullable = false)
	private UUID budgetItemId;

	@Column(length = 200)
	private String description;

	@Column(precision = 18, scale = 4)
	private BigDecimal length;

	@Column(precision = 18, scale = 4)
	private BigDecimal width;

	@Column(precision = 18, scale = 4)
	private BigDecimal height;

	@Column(name = "item_count", precision = 18, scale = 4)
	private BigDecimal itemCount;

	@Column(precision = 18, scale = 6)
	private BigDecimal factor;

	@Column(length = 500)
	private String formula;

	@Column(precision = 18, scale = 4)
	private BigDecimal result;

	@Column(length = 500)
	private String notes;

	@Column(nullable = false)
	private int position;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected Measurement() {
	}

	public Measurement(UUID id, UUID budgetItemId, int position) {
		this.id = id;
		this.budgetItemId = budgetItemId;
		this.position = position;
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

	public void setDimensions(String description, BigDecimal length, BigDecimal width, BigDecimal height,
			BigDecimal itemCount, BigDecimal factor) {
		this.description = description;
		this.length = length;
		this.width = width;
		this.height = height;
		this.itemCount = itemCount;
		this.factor = factor;
	}

	public void recordResult(String formula, BigDecimal result, String notes) {
		this.formula = formula;
		this.result = result;
		this.notes = notes;
	}

	public UUID id() {
		return id;
	}

	public UUID budgetItemId() {
		return budgetItemId;
	}

	public BigDecimal result() {
		return result;
	}

	public int position() {
		return position;
	}

	public String description() {
		return description;
	}

	public BigDecimal length() {
		return length;
	}

	public BigDecimal width() {
		return width;
	}

	public BigDecimal height() {
		return height;
	}

	public BigDecimal itemCount() {
		return itemCount;
	}

	public BigDecimal factor() {
		return factor;
	}

	public String formula() {
		return formula;
	}

	public String notes() {
		return notes;
	}
}
