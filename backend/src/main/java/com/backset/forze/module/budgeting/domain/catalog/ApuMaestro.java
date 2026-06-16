package com.backset.forze.module.budgeting.domain.catalog;

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
 * Master APU (design section 10.2). Carries its own catalog version number and lifecycle status.
 */
@Entity
@Table(name = "budgeting_apu_maestros")
public class ApuMaestro {

	@Id
	private UUID id;

	@Column(name = "organization_id", nullable = false)
	private UUID organizationId;

	@Column(nullable = false, length = 60)
	private String code;

	@Column(nullable = false, length = 200)
	private String name;

	@Column(name = "unit_id", nullable = false)
	private UUID unitId;

	@Column(name = "category_id")
	private UUID categoryId;

	@Column(name = "version_number", nullable = false)
	private int versionNumber;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ApuStatus status;

	@Column(precision = 18, scale = 6)
	private BigDecimal yield;

	@Column(name = "estimated_cost", precision = 18, scale = 2)
	private BigDecimal estimatedCost;

	@Column(name = "estimated_cost_currency", length = 3)
	private String estimatedCostCurrency;

	@Column(name = "valid_until")
	private LocalDate validUntil;

	@Column(name = "author_user_id")
	private UUID authorUserId;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	private long version;

	protected ApuMaestro() {
	}

	public ApuMaestro(UUID id, UUID organizationId, String code, String name, UUID unitId, int versionNumber) {
		this.id = id;
		this.organizationId = organizationId;
		this.code = code;
		this.name = name;
		this.unitId = unitId;
		this.versionNumber = versionNumber;
		this.status = ApuStatus.BORRADOR;
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

	public void changeStatus(ApuStatus status) {
		this.status = status;
	}

	public void updateEstimate(BigDecimal yield, BigDecimal estimatedCost, String currencyCode, LocalDate validUntil) {
		this.yield = yield;
		this.estimatedCost = estimatedCost;
		this.estimatedCostCurrency = currencyCode;
		this.validUntil = validUntil;
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

	public UUID unitId() {
		return unitId;
	}

	public int versionNumber() {
		return versionNumber;
	}

	public ApuStatus status() {
		return status;
	}

	public BigDecimal yield() {
		return yield;
	}

	public BigDecimal estimatedCost() {
		return estimatedCost;
	}

	public LocalDate validUntil() {
		return validUntil;
	}
}
