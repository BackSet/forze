package com.backset.forze.module.budgeting.domain.admin;

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
 * Unit of measure configured per organization (design sections 2.1 and 10).
 */
@Entity
@Table(name = "budgeting_units_of_measure")
public class UnitOfMeasure {

	@Id
	private UUID id;

	@Column(name = "organization_id", nullable = false)
	private UUID organizationId;

	@Column(nullable = false, length = 40)
	private String code;

	@Column(nullable = false, length = 120)
	private String name;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	private long version;

	protected UnitOfMeasure() {
	}

	public UnitOfMeasure(UUID id, UUID organizationId, String code, String name) {
		this.id = id;
		this.organizationId = organizationId;
		this.code = code;
		this.name = name;
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
}
