package com.backset.forze.module.budgeting.domain.project;

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
 * Project aggregate root (design section 6.2). current_budget_id is a plain reference to the
 * live/approved budget to avoid a projects&lt;-&gt;budgets cycle.
 */
@Entity
@Table(name = "budgeting_projects")
public class Project {

	@Id
	private UUID id;

	@Column(name = "organization_id", nullable = false)
	private UUID organizationId;

	@Column(nullable = false, length = 60)
	private String code;

	@Column(nullable = false, length = 200)
	private String name;

	@Column(name = "client_id")
	private UUID clientId;

	@Column(columnDefinition = "text")
	private String description;

	@Column(name = "work_type", length = 120)
	private String workType;

	@Column(length = 200)
	private String location;

	@Column(name = "estimated_start_date")
	private LocalDate estimatedStartDate;

	@Column(name = "estimated_end_date")
	private LocalDate estimatedEndDate;

	@Column(name = "currency_code", nullable = false, length = 3)
	private String currencyCode;

	@Column(name = "target_amount", precision = 18, scale = 2)
	private BigDecimal targetAmount;

	@Column(name = "minimum_margin", precision = 7, scale = 4)
	private BigDecimal minimumMargin;

	@Column(name = "responsible_user_id")
	private UUID responsibleUserId;

	@Column(name = "current_budget_id")
	private UUID currentBudgetId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ProjectStatus status;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	private long version;

	protected Project() {
	}

	public Project(UUID id, UUID organizationId, String code, String name, String currencyCode) {
		this.id = id;
		this.organizationId = organizationId;
		this.code = code;
		this.name = name;
		this.currencyCode = currencyCode;
		this.status = ProjectStatus.ACTIVO;
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

	public void describe(UUID clientId, String description, String workType, String location,
			LocalDate estimatedStartDate, LocalDate estimatedEndDate) {
		this.clientId = clientId;
		this.description = description;
		this.workType = workType;
		this.location = location;
		this.estimatedStartDate = estimatedStartDate;
		this.estimatedEndDate = estimatedEndDate;
	}

	public void setFinancialTarget(BigDecimal targetAmount, BigDecimal minimumMargin) {
		this.targetAmount = targetAmount;
		this.minimumMargin = minimumMargin;
	}

	public void assignResponsible(UUID responsibleUserId) {
		this.responsibleUserId = responsibleUserId;
	}

	public void setCurrentBudget(UUID currentBudgetId) {
		this.currentBudgetId = currentBudgetId;
	}

	public void archive() {
		this.status = ProjectStatus.ARCHIVADO;
	}

	public UUID id() {
		return id;
	}

	public UUID organizationId() {
		return organizationId;
	}

	public String code() {
		return code;
	}

	public String name() {
		return name;
	}

	public UUID clientId() {
		return clientId;
	}

	public String currencyCode() {
		return currencyCode;
	}

	public BigDecimal targetAmount() {
		return targetAmount;
	}

	public BigDecimal minimumMargin() {
		return minimumMargin;
	}

	public UUID responsibleUserId() {
		return responsibleUserId;
	}

	public UUID currentBudgetId() {
		return currentBudgetId;
	}

	public ProjectStatus status() {
		return status;
	}

	public String description() {
		return description;
	}

	public String workType() {
		return workType;
	}

	public String location() {
		return location;
	}

	public LocalDate estimatedStartDate() {
		return estimatedStartDate;
	}

	public LocalDate estimatedEndDate() {
		return estimatedEndDate;
	}
}
