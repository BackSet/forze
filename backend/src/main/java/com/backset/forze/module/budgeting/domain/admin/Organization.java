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
 * Tenant root for budgeting data (design sections 2.1 and 4: organization selector).
 */
@Entity
@Table(name = "budgeting_organizations")
public class Organization {

	@Id
	private UUID id;

	@Column(nullable = false, length = 160)
	private String name;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	private long version;

	protected Organization() {
	}

	public Organization(UUID id, String name) {
		this.id = id;
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

	public void rename(String name) {
		this.name = name;
	}

	public UUID id() {
		return id;
	}

	public String name() {
		return name;
	}
}
