package com.backset.forze.module.budgeting.infrastructure;

import java.util.List;
import java.util.UUID;

import com.backset.forze.module.budgeting.domain.supplier.QuotationItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuotationItemRepository extends JpaRepository<QuotationItem, UUID> {

	List<QuotationItem> findByQuotationIdOrderByPosition(UUID quotationId);
}
