package com.backset.forze.module.budgeting.domain.budget;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * Chapter or subchapter inside a budget version (design section 8.4). A subchapter references
 * its parent via parent_chapter_id. Child of the {@link BudgetVersion} aggregate.
 */
@Entity
@Table(name = "budgeting_chapters")
public class Chapter {

	@Id
	private UUID id;

	@Column(name = "budget_version_id", nullable = false)
	private UUID budgetVersionId;

	@Column(name = "parent_chapter_id")
	private UUID parentChapterId;

	@Column(length = 60)
	private String code;

	@Column(nullable = false, length = 200)
	private String name;

	@Column(nullable = false)
	private int position;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected Chapter() {
	}

	public Chapter(UUID id, UUID budgetVersionId, String name, int position) {
		this.id = id;
		this.budgetVersionId = budgetVersionId;
		this.name = name;
		this.position = position;
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

	public void setParent(UUID parentChapterId) {
		this.parentChapterId = parentChapterId;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public void rename(String name) {
		this.name = name;
	}

	public void moveTo(int position) {
		this.position = position;
	}

	public UUID id() {
		return id;
	}

	public UUID budgetVersionId() {
		return budgetVersionId;
	}

	public UUID parentChapterId() {
		return parentChapterId;
	}

	public String code() {
		return code;
	}

	public String name() {
		return name;
	}

	public int position() {
		return position;
	}
}
