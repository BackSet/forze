package com.backset.forze.module.budgeting.domain.audit;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * Append-only audit record (design section 21).
 */
@Entity
@Table(name = "budgeting_audit_log")
public class AuditLogEntry {

	@Id
	private UUID id;

	@Column(name = "organization_id")
	private UUID organizationId;

	@Column(name = "user_id")
	private UUID userId;

	@Column(nullable = false, length = 120)
	private String action;

	@Column(name = "entity_type", nullable = false, length = 120)
	private String entityType;

	@Column(name = "entity_id")
	private UUID entityId;

	@Column(name = "old_value", columnDefinition = "text")
	private String oldValue;

	@Column(name = "new_value", columnDefinition = "text")
	private String newValue;

	@Column(columnDefinition = "text")
	private String reason;

	@Column(name = "ip_address", length = 64)
	private String ipAddress;

	@Column(name = "occurred_at", nullable = false)
	private Instant occurredAt;

	protected AuditLogEntry() {
	}

	public AuditLogEntry(UUID id, UUID organizationId, UUID userId, String action, String entityType, UUID entityId,
			Instant occurredAt) {
		this.id = id;
		this.organizationId = organizationId;
		this.userId = userId;
		this.action = action;
		this.entityType = entityType;
		this.entityId = entityId;
		this.occurredAt = occurredAt;
	}

	@PrePersist
	void onCreate() {
		if (occurredAt == null) {
			occurredAt = Instant.now();
		}
	}

	public void withChange(String oldValue, String newValue, String reason, String ipAddress) {
		this.oldValue = oldValue;
		this.newValue = newValue;
		this.reason = reason;
		this.ipAddress = ipAddress;
	}

	public UUID id() {
		return id;
	}

	public UUID organizationId() {
		return organizationId;
	}

	public String action() {
		return action;
	}

	public String entityType() {
		return entityType;
	}

	public UUID entityId() {
		return entityId;
	}

	public Instant occurredAt() {
		return occurredAt;
	}
}
