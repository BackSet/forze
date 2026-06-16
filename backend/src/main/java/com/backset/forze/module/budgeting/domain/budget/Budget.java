package com.backset.forze.module.budgeting.domain.budget;

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
 * Budget container for a project (design section 7). Holds one or more versions; a single budget
 * operates in one currency (design "Moneda" per budget).
 */
@Entity
@Table(name = "budgeting_budgets")
public class Budget {

	@Id
	private UUID id;

	@Column(name = "organization_id", nullable = false)
	private UUID organizationId;

	@Column(name = "project_id", nullable = false)
	private UUID projectId;

	@Column(nullable = false, length = 60)
	private String code;

	@Column(nullable = false, length = 200)
	private String name;

	@Column(name = "currency_code", nullable = false, length = 3)
	private String currencyCode;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	private long version;

	protected Budget() {
	}

	public Budget(UUID id, UUID organizationId, UUID projectId, String code, String name, String currencyCode) {
		this.id = id;
		this.organizationId = organizationId;
		this.projectId = projectId;
		this.code = code;
		this.name = name;
		this.currencyCode = currencyCode;
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

	public UUID projectId() {
		return projectId;
	}

	public String code() {
		return code;
	}

	public String name() {
		return name;
	}

	public String currencyCode() {
		return currencyCode;
	}
}
