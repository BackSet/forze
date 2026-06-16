package com.backset.forze.module.budgeting.domain.document;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * Client document generated from a budget version (design section 17).
 */
@Entity
@Table(name = "budgeting_documents")
public class BudgetDocument {

	@Id
	private UUID id;

	@Column(name = "organization_id", nullable = false)
	private UUID organizationId;

	@Column(name = "budget_version_id", nullable = false)
	private UUID budgetVersionId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private DocumentType type;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private DocumentFormat format;

	@Column(length = 60)
	private String number;

	@Column(name = "valid_until")
	private LocalDate validUntil;

	@Column(columnDefinition = "text")
	private String notes;

	@Column(name = "generated_by_user_id")
	private UUID generatedByUserId;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected BudgetDocument() {
	}

	public BudgetDocument(UUID id, UUID organizationId, UUID budgetVersionId, DocumentType type,
			DocumentFormat format) {
		this.id = id;
		this.organizationId = organizationId;
		this.budgetVersionId = budgetVersionId;
		this.type = type;
		this.format = format;
	}

	@PrePersist
	void onCreate() {
		createdAt = Instant.now();
	}

	public void withMetadata(String number, LocalDate validUntil, String notes, UUID generatedByUserId) {
		this.number = number;
		this.validUntil = validUntil;
		this.notes = notes;
		this.generatedByUserId = generatedByUserId;
	}

	public UUID id() {
		return id;
	}

	public UUID organizationId() {
		return organizationId;
	}

	public UUID budgetVersionId() {
		return budgetVersionId;
	}

	public DocumentType type() {
		return type;
	}

	public DocumentFormat format() {
		return format;
	}

	public String number() {
		return number;
	}
}
