package com.backset.forze.module.budgeting.domain.admin;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * A role within RBAC. System roles are global (organizationId == null) and
 * cannot be edited or deleted. Custom roles belong to a single organization.
 * A role flagged {@code allPermissions} (the ADMINISTRADOR system role) always
 * grants every registered permission, including ones added later.
 */
@Entity
@Table(name = "budgeting_roles")
public class Role {

	@Id
	private UUID id;

	@Column(name = "organization_id")
	private UUID organizationId;

	@Column(nullable = false)
	private String code;

	@Column(nullable = false)
	private String name;

	@Column(name = "is_system", nullable = false)
	private boolean system;

	@Column(name = "all_permissions", nullable = false)
	private boolean allPermissions;

	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(
			name = "budgeting_role_permissions",
			joinColumns = @JoinColumn(name = "role_id"),
			inverseJoinColumns = @JoinColumn(name = "permission_id"))
	private Set<Permission> permissions = new HashSet<>();

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	private long version;

	protected Role() {
	}

	public Role(UUID id, UUID organizationId, String code, String name, boolean system, boolean allPermissions) {
		this.id = id;
		this.organizationId = organizationId;
		this.code = code;
		this.name = name;
		this.system = system;
		this.allPermissions = allPermissions;
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

	public String code() {
		return code;
	}

	public String name() {
		return name;
	}

	public boolean isSystem() {
		return system;
	}

	public boolean grantsAllPermissions() {
		return allPermissions;
	}

	public Set<Permission> permissions() {
		return permissions;
	}

	public void rename(String name) {
		this.name = name;
	}

	public void replacePermissions(Set<Permission> newPermissions) {
		this.permissions = new HashSet<>(newPermissions);
	}
}
