package com.backset.forze.module.budgeting.domain.supplier;

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
 * Supplier (design section 12.1).
 */
@Entity
@Table(name = "budgeting_suppliers")
public class Supplier {

	@Id
	private UUID id;

	@Column(name = "organization_id", nullable = false)
	private UUID organizationId;

	@Column(name = "legal_name", nullable = false, length = 200)
	private String legalName;

	@Column(name = "tax_id", length = 40)
	private String taxId;

	@Column(name = "contact_name", length = 160)
	private String contactName;

	@Column(length = 60)
	private String phone;

	@Column(length = 254)
	private String email;

	@Column(length = 120)
	private String city;

	@Column(name = "offered_products", columnDefinition = "text")
	private String offeredProducts;

	@Column(name = "payment_terms", length = 200)
	private String paymentTerms;

	@Column(name = "delivery_time", length = 120)
	private String deliveryTime;

	@Column(precision = 3, scale = 2)
	private BigDecimal rating;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private SupplierStatus status;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	private long version;

	protected Supplier() {
	}

	public Supplier(UUID id, UUID organizationId, String legalName) {
		this.id = id;
		this.organizationId = organizationId;
		this.legalName = legalName;
		this.status = SupplierStatus.ACTIVO;
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

	public void updateContact(String taxId, String contactName, String phone, String email, String city) {
		this.taxId = taxId;
		this.contactName = contactName;
		this.phone = phone;
		this.email = email;
		this.city = city;
	}

	public void updateCommercialTerms(String offeredProducts, String paymentTerms, String deliveryTime,
			BigDecimal rating) {
		this.offeredProducts = offeredProducts;
		this.paymentTerms = paymentTerms;
		this.deliveryTime = deliveryTime;
		this.rating = rating;
	}

	public void deactivate() {
		this.status = SupplierStatus.INACTIVO;
	}

	public UUID id() {
		return id;
	}

	public UUID organizationId() {
		return organizationId;
	}

	public String legalName() {
		return legalName;
	}

	public String taxId() {
		return taxId;
	}

	public SupplierStatus status() {
		return status;
	}

	public BigDecimal rating() {
		return rating;
	}
}
