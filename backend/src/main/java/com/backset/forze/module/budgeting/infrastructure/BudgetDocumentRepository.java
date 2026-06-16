package com.backset.forze.module.budgeting.infrastructure;

import java.util.List;
import java.util.UUID;

import com.backset.forze.module.budgeting.domain.document.BudgetDocument;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetDocumentRepository extends JpaRepository<BudgetDocument, UUID> {

	List<BudgetDocument> findByBudgetVersionId(UUID budgetVersionId);
}
