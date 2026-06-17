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

/**
 * Component of a master APU (design sections 9.2 and 10.2). Child of the {@link ApuMaestro} aggregate.
 */
@Entity
@Table(name = "budgeting_apu_components")
public class ApuComponent {

	@Id
	private UUID id;

	@Column(name = "apu_maestro_id", nullable = false)
	private UUID apuMaestroId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ComponentSection section;

	@Column(name = "insumo_id")
	private UUID insumoId;

	@Column(length = 200)
	private String description;

	@Column(name = "unit_id", nullable = false)
	private UUID unitId;

	@Column(nullable = false, precision = 18, scale = 4)
	private BigDecimal quantity;

	@Column(precision = 18, scale = 6)
	private BigDecimal yield;

	@Column(name = "unit_price", nullable = false, precision = 18, scale = 4)
	private BigDecimal unitPrice;

	@Column(name = "waste_factor", precision = 18, scale = 6)
	private BigDecimal wasteFactor;

	@Column(nullable = false)
	private int position;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected ApuComponent() {
	}

	public ApuComponent(UUID id, UUID apuMaestroId, ComponentSection section, UUID unitId,
			BigDecimal quantity, BigDecimal unitPrice, int position) {
		this.id = id;
		this.apuMaestroId = apuMaestroId;
		this.section = section;
		this.unitId = unitId;
		this.quantity = quantity;
		this.unitPrice = unitPrice;
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

	public void describe(UUID insumoId, String description, BigDecimal yield, BigDecimal wasteFactor) {
		this.insumoId = insumoId;
		this.description = description;
		this.yield = yield;
		this.wasteFactor = wasteFactor;
	}

	public UUID id() {
		return id;
	}

	public UUID apuMaestroId() {
		return apuMaestroId;
	}

	public ComponentSection section() {
		return section;
	}

	public UUID insumoId() {
		return insumoId;
	}

	public UUID unitId() {
		return unitId;
	}

	public BigDecimal quantity() {
		return quantity;
	}

	public BigDecimal yield() {
		return yield;
	}

	public BigDecimal unitPrice() {
		return unitPrice;
	}

	public BigDecimal wasteFactor() {
		return wasteFactor;
	}

	public int position() {
		return position;
	}

	public String description() {
		return description;
	}
}
