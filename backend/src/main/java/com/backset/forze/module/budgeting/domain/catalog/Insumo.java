package com.backset.forze.module.budgeting.domain.catalog;

import java.math.BigDecimal;
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
 * Catalog insumo (design section 10.3). Foreign keys are stored as plain ids to keep the
 * catalog aggregate decoupled; relational integrity is enforced in the schema.
 */
@Entity
@Table(name = "budgeting_insumos")
public class Insumo {

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

	@Column(name = "unit_id", nullable = false)
	private UUID unitId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private InsumoType type;

	@Column(name = "category_id")
	private UUID categoryId;

	@Column(length = 120)
	private String brand;

	@Column(columnDefinition = "text")
	private String specification;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private CatalogStatus status;

	@Column(name = "reference_price", precision = 18, scale = 4)
	private BigDecimal referencePrice;

	@Column(name = "reference_price_currency", length = 3)
	private String referencePriceCurrency;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	private long version;

	protected Insumo() {
	}

	public Insumo(UUID id, UUID organizationId, String code, String name, UUID unitId, InsumoType type) {
		this.id = id;
		this.organizationId = organizationId;
		this.code = code;
		this.name = name;
		this.unitId = unitId;
		this.type = type;
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

	public void describe(String description, String brand, String specification, UUID categoryId) {
		this.description = description;
		this.brand = brand;
		this.specification = specification;
		this.categoryId = categoryId;
	}

	public void updateReferencePrice(BigDecimal price, String currencyCode) {
		this.referencePrice = price;
		this.referencePriceCurrency = currencyCode;
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

	public InsumoType type() {
		return type;
	}

	public UUID categoryId() {
		return categoryId;
	}

	public CatalogStatus status() {
		return status;
	}

	public BigDecimal referencePrice() {
		return referencePrice;
	}

	public String referencePriceCurrency() {
		return referencePriceCurrency;
	}
}
