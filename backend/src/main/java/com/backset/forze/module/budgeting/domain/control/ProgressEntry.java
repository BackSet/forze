package com.backset.forze.module.budgeting.domain.control;

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
 * Physical progress entry per item (design section 18.2). The progress percentage is derived from
 * budgeted/executed quantities and is not persisted.
 */
@Entity
@Table(name = "budgeting_progress_entries")
public class ProgressEntry {

	@Id
	private UUID id;

	@Column(name = "baseline_id", nullable = false)
	private UUID baselineId;

	@Column(name = "budget_item_id", nullable = false)
	private UUID budgetItemId;

	@Column(name = "budgeted_quantity", nullable = false, precision = 18, scale = 4)
	private BigDecimal budgetedQuantity;

	@Column(name = "executed_quantity", nullable = false, precision = 18, scale = 4)
	private BigDecimal executedQuantity;

	@Column(name = "entry_date", nullable = false)
	private LocalDate entryDate;

	@Column(name = "responsible_user_id")
	private UUID responsibleUserId;

	@Column(name = "evidence_ref", length = 500)
	private String evidenceRef;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected ProgressEntry() {
	}

	public ProgressEntry(UUID id, UUID baselineId, UUID budgetItemId, BigDecimal budgetedQuantity,
			BigDecimal executedQuantity, LocalDate entryDate) {
		this.id = id;
		this.baselineId = baselineId;
		this.budgetItemId = budgetItemId;
		this.budgetedQuantity = budgetedQuantity;
		this.executedQuantity = executedQuantity;
		this.entryDate = entryDate;
	}

	@PrePersist
	void onCreate() {
		createdAt = Instant.now();
	}

	public void withEvidence(UUID responsibleUserId, String evidenceRef) {
		this.responsibleUserId = responsibleUserId;
		this.evidenceRef = evidenceRef;
	}

	public UUID id() {
		return id;
	}

	public UUID baselineId() {
		return baselineId;
	}

	public UUID budgetItemId() {
		return budgetItemId;
	}

	public BigDecimal budgetedQuantity() {
		return budgetedQuantity;
	}

	public BigDecimal executedQuantity() {
		return executedQuantity;
	}

	public LocalDate entryDate() {
		return entryDate;
	}
}
