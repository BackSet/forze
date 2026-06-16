package com.backset.forze.module.budgeting.domain.scenario;

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
 * Budget scenario (design section 13). Belongs to a budget version; stored totals are the
 * comparison snapshot shown in the scenario comparator.
 */
@Entity
@Table(name = "budgeting_scenarios")
public class Scenario {

	@Id
	private UUID id;

	@Column(name = "budget_version_id", nullable = false)
	private UUID budgetVersionId;

	@Column(nullable = false, length = 200)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ScenarioType type;

	@Column(name = "is_primary", nullable = false)
	private boolean primary;

	@Column(name = "utility_rate", precision = 7, scale = 4)
	private BigDecimal utilityRate;

	@Column(name = "indirect_rate", precision = 7, scale = 4)
	private BigDecimal indirectRate;

	@Column(name = "contingency_rate", precision = 7, scale = 4)
	private BigDecimal contingencyRate;

	@Column(name = "duration_days")
	private Integer durationDays;

	@Column(name = "construction_method", length = 200)
	private String constructionMethod;

	@Column(name = "total_cost", precision = 18, scale = 2)
	private BigDecimal totalCost;

	@Column(name = "sale_price", precision = 18, scale = 2)
	private BigDecimal salePrice;

	@Column(precision = 7, scale = 4)
	private BigDecimal margin;

	@Enumerated(EnumType.STRING)
	@Column(length = 10)
	private RiskLevel risk;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	private long version;

	protected Scenario() {
	}

	public Scenario(UUID id, UUID budgetVersionId, String name, ScenarioType type) {
		this.id = id;
		this.budgetVersionId = budgetVersionId;
		this.name = name;
		this.type = type;
		this.primary = false;
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

	public void configure(BigDecimal utilityRate, BigDecimal indirectRate, BigDecimal contingencyRate,
			Integer durationDays, String constructionMethod) {
		this.utilityRate = utilityRate;
		this.indirectRate = indirectRate;
		this.contingencyRate = contingencyRate;
		this.durationDays = durationDays;
		this.constructionMethod = constructionMethod;
	}

	public void recordComparison(BigDecimal totalCost, BigDecimal salePrice, BigDecimal margin, RiskLevel risk) {
		this.totalCost = totalCost;
		this.salePrice = salePrice;
		this.margin = margin;
		this.risk = risk;
	}

	public void makePrimary() {
		this.primary = true;
	}

	public void unsetPrimary() {
		this.primary = false;
	}

	public UUID id() {
		return id;
	}

	public UUID budgetVersionId() {
		return budgetVersionId;
	}

	public String name() {
		return name;
	}

	public ScenarioType type() {
		return type;
	}

	public boolean primary() {
		return primary;
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

	public RiskLevel risk() {
		return risk;
	}
}
