package com.backset.forze.module.budgeting.domain.approval;

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
 * Approval request targeting a budget version (design section 15).
 */
@Entity
@Table(name = "budgeting_approval_requests")
public class ApprovalRequest {

	@Id
	private UUID id;

	@Column(name = "budget_version_id", nullable = false)
	private UUID budgetVersionId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private ApprovalStatus status;

	@Column(name = "submitted_by_user_id")
	private UUID submittedByUserId;

	@Column(name = "submitted_at", nullable = false)
	private Instant submittedAt;

	@Column(name = "decided_by_user_id")
	private UUID decidedByUserId;

	@Column(name = "decided_at")
	private Instant decidedAt;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	private long version;

	protected ApprovalRequest() {
	}

	public ApprovalRequest(UUID id, UUID budgetVersionId, UUID submittedByUserId, Instant submittedAt) {
		this.id = id;
		this.budgetVersionId = budgetVersionId;
		this.submittedByUserId = submittedByUserId;
		this.submittedAt = submittedAt;
		this.status = ApprovalStatus.PENDIENTE_APROBACION;
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

	private void decide(ApprovalStatus status, UUID decidedByUserId, Instant decidedAt) {
		this.status = status;
		this.decidedByUserId = decidedByUserId;
		this.decidedAt = decidedAt;
	}

	public void approve(UUID decidedByUserId, Instant decidedAt) {
		decide(ApprovalStatus.APROBADO, decidedByUserId, decidedAt);
	}

	public void observe(UUID decidedByUserId, Instant decidedAt) {
		decide(ApprovalStatus.OBSERVADO, decidedByUserId, decidedAt);
	}

	public void reject(UUID decidedByUserId, Instant decidedAt) {
		decide(ApprovalStatus.RECHAZADO, decidedByUserId, decidedAt);
	}

	public UUID id() {
		return id;
	}

	public UUID budgetVersionId() {
		return budgetVersionId;
	}

	public ApprovalStatus status() {
		return status;
	}

	public UUID decidedByUserId() {
		return decidedByUserId;
	}

	public Instant decidedAt() {
		return decidedAt;
	}
}
