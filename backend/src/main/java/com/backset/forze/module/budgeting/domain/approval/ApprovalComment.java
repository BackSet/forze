package com.backset.forze.module.budgeting.domain.approval;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * Approval comment (design section 15). Optionally references a budget item ("rubro relacionado").
 * Child of the {@link ApprovalRequest} aggregate.
 */
@Entity
@Table(name = "budgeting_approval_comments")
public class ApprovalComment {

	@Id
	private UUID id;

	@Column(name = "approval_request_id", nullable = false)
	private UUID approvalRequestId;

	@Column(name = "budget_item_id")
	private UUID budgetItemId;

	@Column(name = "author_user_id")
	private UUID authorUserId;

	@Column(nullable = false, columnDefinition = "text")
	private String comment;

	@Column(columnDefinition = "text")
	private String response;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected ApprovalComment() {
	}

	public ApprovalComment(UUID id, UUID approvalRequestId, UUID authorUserId, String comment) {
		this.id = id;
		this.approvalRequestId = approvalRequestId;
		this.authorUserId = authorUserId;
		this.comment = comment;
	}

	@PrePersist
	void onCreate() {
		createdAt = Instant.now();
	}

	public void relateToItem(UUID budgetItemId) {
		this.budgetItemId = budgetItemId;
	}

	public void respond(String response) {
		this.response = response;
	}

	public UUID id() {
		return id;
	}

	public UUID approvalRequestId() {
		return approvalRequestId;
	}

	public UUID budgetItemId() {
		return budgetItemId;
	}

	public String comment() {
		return comment;
	}

	public String response() {
		return response;
	}
}
