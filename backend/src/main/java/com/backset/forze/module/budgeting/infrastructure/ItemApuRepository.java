package com.backset.forze.module.budgeting.infrastructure;

import java.util.Optional;
import java.util.UUID;

import com.backset.forze.module.budgeting.domain.budget.ItemApu;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemApuRepository extends JpaRepository<ItemApu, UUID> {

	Optional<ItemApu> findByBudgetItemId(UUID budgetItemId);
}
