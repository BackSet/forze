package com.backset.forze.module.budgeting.infrastructure;

import java.util.List;
import java.util.UUID;

import com.backset.forze.module.budgeting.domain.supplier.Quotation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuotationRepository extends JpaRepository<Quotation, UUID> {

	List<Quotation> findBySupplierId(UUID supplierId);
}
