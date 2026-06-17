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
 * Budget Risk record (design section 13/Entrega A).
 * Represents a identified threat or risk associated with a budget version.
 */
@Entity
@Table(name = "budgeting_budget_risks")
public class BudgetRisk {

	@Id
	private UUID id;

	@Column(name = "organization_id", nullable = false)
	private UUID organizationId;

	@Column(name = "budget_version_id", nullable = false)
	private UUID budgetVersionId;

	@Column(nullable = false, columnDefinition = "text")
	private String description;

	@Column(nullable = false, precision = 5, scale = 4)
	private BigDecimal probability;

	@Column(nullable = false, precision = 18, scale = 2)
	private BigDecimal impact;

	@Column(name = "expected_amount", nullable = false, precision = 18, scale = 2)
	private BigDecimal expectedAmount;

	@Column(name = "assigned_to", length = 160)
	private String assignedTo;

	@Column(columnDefinition = "text")
	private String mitigation;

	@Column(nullable = false)
	private boolean mitigated;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected BudgetRisk() {
	}

	public BudgetRisk(UUID id, UUID organizationId, UUID budgetVersionId, String description, BigDecimal probability, BigDecimal impact) {
		this.id = id;
		this.organizationId = organizationId;
		this.budgetVersionId = budgetVersionId;
		this.description = description;
		this.probability = probability;
		this.impact = impact;
		this.expectedAmount = probability.multiply(impact).setScale(2, java.math.RoundingMode.HALF_UP);
		this.mitigated = false;
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

	public void updateDetails(String description, BigDecimal probability, BigDecimal impact, String assignedTo, String mitigation, boolean mitigated) {
		this.description = description;
		this.probability = probability;
		this.impact = impact;
		this.expectedAmount = probability.multiply(impact).setScale(2, java.math.RoundingMode.HALF_UP);
		this.assignedTo = assignedTo;
		this.mitigation = mitigation;
		this.mitigated = mitigated;
	}

	public UUID id() {
		return id;
	}

	public UUID organizationId() {
		return organizationId;
	}

	public UUID budgetVersionId() {
		return budgetVersionId;
	}

	public String description() {
		return description;
	}

	public BigDecimal probability() {
		return probability;
	}

	public BigDecimal impact() {
		return impact;
	}

	public BigDecimal expectedAmount() {
		return expectedAmount;
	}

	public String assignedTo() {
		return assignedTo;
	}

	public String mitigation() {
		return mitigation;
	}

	public boolean mitigated() {
		return mitigated;
	}
}
