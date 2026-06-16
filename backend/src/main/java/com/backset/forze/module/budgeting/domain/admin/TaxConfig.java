package com.backset.forze.module.budgeting.domain.admin;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Configurable tax with a percentage rate (design section 2.1 "configurar impuestos").
 */
@Entity
@Table(name = "budgeting_tax_configs")
public class TaxConfig {

	@Id
	private UUID id;

	@Column(name = "organization_id", nullable = false)
	private UUID organizationId;

	@Column(nullable = false, length = 40)
	private String code;

	@Column(nullable = false, length = 120)
	private String name;

	@Column(nullable = false, precision = 7, scale = 4)
	private BigDecimal rate;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	private long version;

	protected TaxConfig() {
	}

	public TaxConfig(UUID id, UUID organizationId, String code, String name, BigDecimal rate) {
		this.id = id;
		this.organizationId = organizationId;
		this.code = code;
		this.name = name;
		this.rate = rate;
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

	public BigDecimal rate() {
		return rate;
	}
}
