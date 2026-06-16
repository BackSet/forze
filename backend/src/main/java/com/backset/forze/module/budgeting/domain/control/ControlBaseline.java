package com.backset.forze.module.budgeting.domain.control;

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
 * Site-control baseline: the approved budget version that becomes the project line base
 * (design section 18.1). One baseline per project.
 */
@Entity
@Table(name = "budgeting_control_baselines")
public class ControlBaseline {

	@Id
	private UUID id;

	@Column(name = "project_id", nullable = false)
	private UUID projectId;

	@Column(name = "budget_version_id", nullable = false)
	private UUID budgetVersionId;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	private long version;

	protected ControlBaseline() {
	}

	public ControlBaseline(UUID id, UUID projectId, UUID budgetVersionId) {
		this.id = id;
		this.projectId = projectId;
		this.budgetVersionId = budgetVersionId;
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

	public UUID projectId() {
		return projectId;
	}

	public UUID budgetVersionId() {
		return budgetVersionId;
	}
}
