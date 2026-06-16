package com.backset.forze.module.budgeting.domain.control;

import java.math.BigDecimal;
import java.time.Instant;
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
 * Additional work item against a baseline (design section 18.5).
 */
@Entity
@Table(name = "budgeting_additionals")
public class Additional {

	@Id
	private UUID id;

	@Column(name = "baseline_id", nullable = false)
	private UUID baselineId;

	@Column(name = "budget_item_id")
	private UUID budgetItemId;

	@Column(nullable = false, columnDefinition = "text")
	private String description;

	@Column(precision = 18, scale = 4)
	private BigDecimal quantity;

	@Column(precision = 18, scale = 2)
	private BigDecimal amount;

	@Column(name = "amount_currency", length = 3)
	private String amountCurrency;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private AdditionalStatus status;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	private long version;

	protected Additional() {
	}

	public Additional(UUID id, UUID baselineId, String description) {
		this.id = id;
		this.baselineId = baselineId;
		this.description = description;
		this.status = AdditionalStatus.PROPUESTO;
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

	public void quantify(UUID budgetItemId, BigDecimal quantity, BigDecimal amount, String amountCurrency) {
		this.budgetItemId = budgetItemId;
		this.quantity = quantity;
		this.amount = amount;
		this.amountCurrency = amountCurrency;
	}

	public void changeStatus(AdditionalStatus status) {
		this.status = status;
	}

	public UUID id() {
		return id;
	}

	public UUID baselineId() {
		return baselineId;
	}

	public String description() {
		return description;
	}

	public BigDecimal amount() {
		return amount;
	}

	public AdditionalStatus status() {
		return status;
	}
}
