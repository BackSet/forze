package com.backset.forze.module.budgeting.domain.admin;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A registered permission in the system. Codes mirror {@code ForzePermission}
 * and are the unit of authorization checked by {@code @PreAuthorize}.
 */
@Entity
@Table(name = "budgeting_permissions")
public class Permission {

	@Id
	private UUID id;

	@Column(nullable = false, unique = true)
	private String code;

	@Column(nullable = false)
	private String area;

	@Column(nullable = false)
	private String description;

	protected Permission() {
	}

	public Permission(UUID id, String code, String area, String description) {
		this.id = id;
		this.code = code;
		this.area = area;
		this.description = description;
	}

	public UUID id() {
		return id;
	}

	public String code() {
		return code;
	}

	public String area() {
		return area;
	}

	public String description() {
		return description;
	}
}
