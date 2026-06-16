package com.backset.forze.module.identity.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "identity_users")
public class UserAccount {

	@Id
	private UUID id;

	@Column(nullable = false, unique = true, length = 80)
	private String username;

	@Column(unique = true, length = 254)
	private String email;

	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

	@Column(nullable = false)
	private boolean enabled;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected UserAccount() {
	}

	public UserAccount(UUID id, String username, String email, String passwordHash, boolean enabled) {
		this.id = id;
		this.username = username;
		this.email = email == null || email.isBlank() ? null : email;
		this.passwordHash = passwordHash;
		this.enabled = enabled;
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

	public String username() {
		return username;
	}

	public String email() {
		return email;
	}

	public String passwordHash() {
		return passwordHash;
	}

	public boolean enabled() {
		return enabled;
	}
}
