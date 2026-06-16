package com.backset.forze.module.budgeting.domain.catalog;

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
 * Master rubro (design section 10.1). base_apu_id is the rubro's base APU.
 */
@Entity
@Table(name = "budgeting_rubros_maestros")
public class RubroMaestro {

	@Id
	private UUID id;

	@Column(name = "organization_id", nullable = false)
	private UUID organizationId;

	@Column(nullable = false, length = 60)
	private String code;

	@Column(nullable = false, length = 200)
	private String name;

	@Column(columnDefinition = "text")
	private String description;

	@Column(name = "category_id")
	private UUID categoryId;

	@Column(name = "unit_id", nullable = false)
	private UUID unitId;

	@Column(columnDefinition = "text")
	private String specification;

	@Column(columnDefinition = "text")
	private String keywords;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private CatalogStatus status;

	@Column(name = "base_apu_id")
	private UUID baseApuId;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	private long version;

	protected RubroMaestro() {
	}

	public RubroMaestro(UUID id, UUID organizationId, String code, String name, UUID unitId) {
		this.id = id;
		this.organizationId = organizationId;
		this.code = code;
		this.name = name;
		this.unitId = unitId;
		this.status = CatalogStatus.ACTIVO;
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

	public void describe(String description, String specification, String keywords, UUID categoryId) {
		this.description = description;
		this.specification = specification;
		this.keywords = keywords;
		this.categoryId = categoryId;
	}

	public void assignBaseApu(UUID baseApuId) {
		this.baseApuId = baseApuId;
	}

	public void archive() {
		this.status = CatalogStatus.ARCHIVADO;
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

	public UUID unitId() {
		return unitId;
	}

	public UUID categoryId() {
		return categoryId;
	}

	public CatalogStatus status() {
		return status;
	}

	public UUID baseApuId() {
		return baseApuId;
	}
}
