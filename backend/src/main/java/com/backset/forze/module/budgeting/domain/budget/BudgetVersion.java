package com.backset.forze.module.budgeting.domain.budget;

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
 * Budget version (design sections 7, 8, 14, 16). This is the editable/approvable unit and the
 * root of the chapter/item/APU snapshot tree. Once {@code APROBADO} it is immutable: any change
 * must create a new version. Stored totals are frozen snapshots kept for historical comparison.
 */
@Entity
@Table(name = "budgeting_budget_versions")
public class BudgetVersion {

	@Id
	private UUID id;

	@Column(name = "budget_id", nullable = false)
	private UUID budgetId;

	@Column(name = "version_number", nullable = false)
	private int versionNumber;

	@Column(length = 200)
	private String name;

	@Column(columnDefinition = "text")
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private BudgetStatus status;

	@Column(name = "change_reason", columnDefinition = "text")
	private String changeReason;

	@Column(name = "created_by_user_id")
	private UUID createdByUserId;

	@Column(name = "target_amount", precision = 18, scale = 2)
	private BigDecimal targetAmount;

	@Column(name = "utility_rate", precision = 7, scale = 4)
	private BigDecimal utilityRate;

	@Column(name = "indirect_rate", precision = 7, scale = 4)
	private BigDecimal indirectRate;

	@Column(name = "contingency_rate", precision = 7, scale = 4)
	private BigDecimal contingencyRate;

	@Column(name = "tax_config_id")
	private UUID taxConfigId;

	@Column(name = "valid_until")
	private LocalDate validUntil;

	@Enumerated(EnumType.STRING)
	@Column(name = "viability_status", length = 20)
	private ViabilityStatus viabilityStatus;

	@Column(name = "total_cost", precision = 18, scale = 2)
	private BigDecimal totalCost;

	@Column(name = "sale_price", precision = 18, scale = 2)
	private BigDecimal salePrice;

	@Column(precision = 7, scale = 4)
	private BigDecimal margin;

	@Column(name = "approved_at")
	private Instant approvedAt;

	@Column(name = "approved_by_user_id")
	private UUID approvedByUserId;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	private long version;

	protected BudgetVersion() {
	}

	public BudgetVersion(UUID id, UUID budgetId, int versionNumber) {
		this.id = id;
		this.budgetId = budgetId;
		this.versionNumber = versionNumber;
		this.status = BudgetStatus.BORRADOR;
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

	private void ensureEditable() {
		if (status == BudgetStatus.APROBADO) {
			throw new IllegalStateException("An approved budget version is immutable");
		}
	}

	public void describe(String name, String description, String changeReason, UUID createdByUserId) {
		ensureEditable();
		this.name = name;
		this.description = description;
		this.changeReason = changeReason;
		this.createdByUserId = createdByUserId;
	}

	public void configureFinancials(BigDecimal targetAmount, BigDecimal utilityRate, BigDecimal indirectRate,
			BigDecimal contingencyRate, UUID taxConfigId, LocalDate validUntil) {
		ensureEditable();
		this.targetAmount = targetAmount;
		this.utilityRate = utilityRate;
		this.indirectRate = indirectRate;
		this.contingencyRate = contingencyRate;
		this.taxConfigId = taxConfigId;
		this.validUntil = validUntil;
	}

	/**
	 * Freezes the computed totals and viability evaluated by the calculation use case.
	 */
	public void recordCalculation(BigDecimal totalCost, BigDecimal salePrice, BigDecimal margin,
			ViabilityStatus viabilityStatus) {
		ensureEditable();
		this.totalCost = totalCost;
		this.salePrice = salePrice;
		this.margin = margin;
		this.viabilityStatus = viabilityStatus;
	}

	public void changeStatus(BudgetStatus status) {
		ensureEditable();
		this.status = status;
	}

	public void approve(UUID approvedByUserId, Instant approvedAt) {
		this.status = BudgetStatus.APROBADO;
		this.approvedByUserId = approvedByUserId;
		this.approvedAt = approvedAt;
	}

	public boolean isApproved() {
		return status == BudgetStatus.APROBADO;
	}

	public UUID id() {
		return id;
	}

	public UUID budgetId() {
		return budgetId;
	}

	public int versionNumber() {
		return versionNumber;
	}

	public String name() {
		return name;
	}

	public BudgetStatus status() {
		return status;
	}

	public BigDecimal targetAmount() {
		return targetAmount;
	}

	public BigDecimal utilityRate() {
		return utilityRate;
	}

	public BigDecimal indirectRate() {
		return indirectRate;
	}

	public BigDecimal contingencyRate() {
		return contingencyRate;
	}

	public ViabilityStatus viabilityStatus() {
		return viabilityStatus;
	}

	public BigDecimal totalCost() {
		return totalCost;
	}

	public BigDecimal salePrice() {
		return salePrice;
	}

	public BigDecimal margin() {
		return margin;
	}

	public Instant approvedAt() {
		return approvedAt;
	}

	public long version() {
		return version;
	}

	public LocalDate validUntil() {
		return validUntil;
	}

	public String description() {
		return description;
	}

	public UUID taxConfigId() {
		return taxConfigId;
	}
}
