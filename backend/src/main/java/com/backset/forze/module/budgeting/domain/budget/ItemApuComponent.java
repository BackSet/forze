package com.backset.forze.module.budgeting.domain.budget;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.backset.forze.module.budgeting.domain.catalog.ComponentSection;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * Snapshot of an APU component inside a budget item (design section 9.2). The unit_price is FROZEN:
 * it is the historical price used in the version and must not change when the catalog or price
 * history is later updated (design section 16). Child of the {@link ItemApu} aggregate.
 */
@Entity
@Table(name = "budgeting_item_apu_components")
public class ItemApuComponent {

	@Id
	private UUID id;

	@Column(name = "item_apu_id", nullable = false)
	private UUID itemApuId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ComponentSection section;

	@Column(name = "source_insumo_id")
	private UUID sourceInsumoId;

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

	@Column(name = "price_locked", nullable = false)
	private boolean priceLocked;

	@Column(name = "price_source", length = 200)
	private String priceSource;

	@Column(name = "line_total", precision = 18, scale = 2)
	private BigDecimal lineTotal;

	@Column(nullable = false)
	private int position;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected ItemApuComponent() {
	}

	public ItemApuComponent(UUID id, UUID itemApuId, ComponentSection section, UUID unitId, BigDecimal quantity,
			BigDecimal unitPrice, int position) {
		this.id = id;
		this.itemApuId = itemApuId;
		this.section = section;
		this.unitId = unitId;
		this.quantity = quantity;
		this.unitPrice = unitPrice;
		this.position = position;
		this.priceLocked = false;
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

	public void describe(UUID sourceInsumoId, String description, BigDecimal yield, BigDecimal wasteFactor,
			String priceSource) {
		this.sourceInsumoId = sourceInsumoId;
		this.description = description;
		this.yield = yield;
		this.wasteFactor = wasteFactor;
		this.priceSource = priceSource;
	}

	public void recordLineTotal(BigDecimal lineTotal) {
		this.lineTotal = lineTotal;
	}

	public void lockPrice() {
		this.priceLocked = true;
	}

	public UUID id() {
		return id;
	}

	public UUID itemApuId() {
		return itemApuId;
	}

	public ComponentSection section() {
		return section;
	}

	public UUID sourceInsumoId() {
		return sourceInsumoId;
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

	public boolean priceLocked() {
		return priceLocked;
	}

	public BigDecimal lineTotal() {
		return lineTotal;
	}

	public int position() {
		return position;
	}
}
