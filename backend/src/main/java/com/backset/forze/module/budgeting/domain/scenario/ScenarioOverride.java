package com.backset.forze.module.budgeting.domain.scenario;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * Per-component override applied by a scenario (design section 13 variables: proveedor, precio,
 * rendimiento, desperdicio). Child of the {@link Scenario} aggregate.
 */
@Entity
@Table(name = "budgeting_scenario_overrides")
public class ScenarioOverride {

	@Id
	private UUID id;

	@Column(name = "scenario_id", nullable = false)
	private UUID scenarioId;

	@Column(name = "item_apu_component_id", nullable = false)
	private UUID itemApuComponentId;

	@Column(name = "supplier_id")
	private UUID supplierId;

	@Column(name = "unit_price", precision = 18, scale = 4)
	private BigDecimal unitPrice;

	@Column(precision = 18, scale = 6)
	private BigDecimal yield;

	@Column(name = "waste_factor", precision = 18, scale = 6)
	private BigDecimal wasteFactor;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected ScenarioOverride() {
	}

	public ScenarioOverride(UUID id, UUID scenarioId, UUID itemApuComponentId) {
		this.id = id;
		this.scenarioId = scenarioId;
		this.itemApuComponentId = itemApuComponentId;
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

	public void apply(UUID supplierId, BigDecimal unitPrice, BigDecimal yield, BigDecimal wasteFactor) {
		this.supplierId = supplierId;
		this.unitPrice = unitPrice;
		this.yield = yield;
		this.wasteFactor = wasteFactor;
	}

	public UUID id() {
		return id;
	}

	public UUID scenarioId() {
		return scenarioId;
	}

	public UUID itemApuComponentId() {
		return itemApuComponentId;
	}

	public UUID supplierId() {
		return supplierId;
	}

	public BigDecimal unitPrice() {
		return unitPrice;
	}
}
