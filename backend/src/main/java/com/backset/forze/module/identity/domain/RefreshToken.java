package com.backset.forze.module.identity.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "identity_refresh_tokens")
public class RefreshToken {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private UserAccount user;

	@Column(name = "token_hash", nullable = false, unique = true, length = 128)
	private String tokenHash;

	@Column(name = "family_id", nullable = false)
	private UUID familyId;

	@Column(name = "issued_at", nullable = false)
	private Instant issuedAt;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "revoked_at")
	private Instant revokedAt;

	@Column(name = "replaced_by_token_id")
	private UUID replacedByTokenId;

	@Column(name = "reuse_detected_at")
	private Instant reuseDetectedAt;

	protected RefreshToken() {
	}

	public RefreshToken(UUID id, UserAccount user, String tokenHash, UUID familyId, Instant issuedAt, Instant expiresAt) {
		this.id = id;
		this.user = user;
		this.tokenHash = tokenHash;
		this.familyId = familyId;
		this.issuedAt = issuedAt;
		this.expiresAt = expiresAt;
	}

	public boolean activeAt(Instant now) {
		return revokedAt == null && reuseDetectedAt == null && expiresAt.isAfter(now);
	}

	public void revoke(Instant now, UUID replacementId) {
		revokedAt = now;
		replacedByTokenId = replacementId;
	}

	public void markReuseDetected(Instant now) {
		reuseDetectedAt = now;
		revokedAt = now;
	}

	public UUID id() {
		return id;
	}

	public UserAccount user() {
		return user;
	}

	public String tokenHash() {
		return tokenHash;
	}

	public UUID familyId() {
		return familyId;
	}

	public Instant expiresAt() {
		return expiresAt;
	}
}
