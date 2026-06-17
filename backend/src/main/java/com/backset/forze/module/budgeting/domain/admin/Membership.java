package com.backset.forze.module.budgeting.domain.admin;

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

@Entity
@Table(name = "budgeting_memberships")
public class Membership {

	@Id
	private UUID id;

	@Column(name = "organization_id", nullable = false)
	private UUID organizationId;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private MembershipRole role;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	private long version;

	protected Membership() {
	}

	public Membership(UUID id, UUID organizationId, UUID userId, MembershipRole role) {
		this.id = id;
		this.organizationId = organizationId;
		this.userId = userId;
		this.role = role;
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

	public UUID userId() {
		return userId;
	}

	public MembershipRole role() {
		return role;
	}

	public void changeRole(MembershipRole role) {
		this.role = role;
	}

	public Instant createdAt() {
		return createdAt;
	}

	public Instant updatedAt() {
		return updatedAt;
	}
}
