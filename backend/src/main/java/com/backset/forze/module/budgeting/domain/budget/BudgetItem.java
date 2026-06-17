package com.backset.forze.module.budgeting.domain.budget;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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
 * Budget item: a rubro snapshot inside a budget version (design sections 8.4 and 9.1).
 * The descriptive and code fields are snapshots; source_rubro_id keeps the catalog link and is
 * cleared if the master rubro is deleted, so the historical line stays reproducible.
 */
@Entity
@Table(name = "budgeting_budget_items")
public class BudgetItem {

	@Id
	private UUID id;

	@Column(name = "budget_version_id", nullable = false)
	private UUID budgetVersionId;

	@Column(name = "chapter_id")
	private UUID chapterId;

	@Column(name = "source_rubro_id")
	private UUID sourceRubroId;

	@Column(length = 60)
	private String code;

	@Column(nullable = false, length = 200)
	private String name;

	@Column(columnDefinition = "text")
	private String description;

	@Column(name = "unit_id", nullable = false)
	private UUID unitId;

	@Column(nullable = false, precision = 18, scale = 4)
	private BigDecimal quantity;

	@Column(name = "unit_cost", precision = 18, scale = 4)
	private BigDecimal unitCost;

	@Column(name = "unit_price", precision = 18, scale = 4)
	private BigDecimal unitPrice;

	@Column(name = "price_locked", nullable = false)
	private boolean priceLocked;

	@Column(name = "total_cost", precision = 18, scale = 2)
	private BigDecimal totalCost;

	@Column(name = "total_sale", precision = 18, scale = 2)
	private BigDecimal totalSale;

	@Column(precision = 7, scale = 4)
	private BigDecimal margin;

	@Column(name = "category_id")
	private UUID categoryId;

	@Column(name = "valid_until")
	private LocalDate validUntil;

	@Enumerated(EnumType.STRING)
	@Column(name = "validation_status", length = 20)
	private ItemValidationStatus validationStatus;

	@Column(columnDefinition = "text")
	private String notes;

	@Column(nullable = false)
	private int position;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected BudgetItem() {
	}

	public BudgetItem(UUID id, UUID budgetVersionId, String name, UUID unitId, BigDecimal quantity, int position) {
		this.id = id;
		this.budgetVersionId = budgetVersionId;
		this.name = name;
		this.unitId = unitId;
		this.quantity = quantity;
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

	public void placeInChapter(UUID chapterId) {
		this.chapterId = chapterId;
	}

	public void linkSource(UUID sourceRubroId, String code, String description, UUID categoryId) {
		this.sourceRubroId = sourceRubroId;
		this.code = code;
		this.description = description;
		this.categoryId = categoryId;
	}

	public void changeQuantity(BigDecimal quantity) {
		this.quantity = quantity;
	}

	/**
	 * Freezes the computed unit and total values for this item.
	 */
	public void recordPricing(BigDecimal unitCost, BigDecimal unitPrice, BigDecimal totalCost, BigDecimal totalSale,
			BigDecimal margin) {
		this.unitCost = unitCost;
		this.unitPrice = unitPrice;
		this.totalCost = totalCost;
		this.totalSale = totalSale;
		this.margin = margin;
	}

	public void lockPrice() {
		this.priceLocked = true;
	}

	public void setValidation(ItemValidationStatus validationStatus, LocalDate validUntil) {
		this.validationStatus = validationStatus;
		this.validUntil = validUntil;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

	public UUID id() {
		return id;
	}

	public UUID budgetVersionId() {
		return budgetVersionId;
	}

	public UUID chapterId() {
		return chapterId;
	}

	public UUID sourceRubroId() {
		return sourceRubroId;
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

	public BigDecimal quantity() {
		return quantity;
	}

	public BigDecimal unitCost() {
		return unitCost;
	}

	public BigDecimal unitPrice() {
		return unitPrice;
	}

	public boolean priceLocked() {
		return priceLocked;
	}

	public BigDecimal totalCost() {
		return totalCost;
	}

	public BigDecimal totalSale() {
		return totalSale;
	}

	public BigDecimal margin() {
		return margin;
	}

	public ItemValidationStatus validationStatus() {
		return validationStatus;
	}

	public int position() {
		return position;
	}

	public String description() {
		return description;
	}

	public UUID categoryId() {
		return categoryId;
	}
}
