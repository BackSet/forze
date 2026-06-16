package com.backset.forze.module.budgeting.domain.project;

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
 * Client referenced by projects (design sections 6 and 24). The design enumerates only the
 * client name, so the entity is kept minimal.
 */
@Entity
@Table(name = "budgeting_clients")
public class Client {

	@Id
	private UUID id;

	@Column(name = "organization_id", nullable = false)
	private UUID organizationId;

	@Column(nullable = false, length = 200)
	private String name;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	private long version;

	protected Client() {
	}

	public Client(UUID id, UUID organizationId, String name) {
		this.id = id;
		this.organizationId = organizationId;
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

	public UUID organizationId() {
		return organizationId;
	}

	public String name() {
		return name;
	}
}
