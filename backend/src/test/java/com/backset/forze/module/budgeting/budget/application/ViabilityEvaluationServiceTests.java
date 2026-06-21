package com.backset.forze.module.budgeting.budget.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.backset.forze.module.budgeting.domain.budget.BudgetVersion;
import com.backset.forze.module.budgeting.domain.budget.ViabilityStatus;
import org.junit.jupiter.api.Test;

/**
 * Boundary (limit) cases for viability: the rules use strict comparisons, so the
 * exact equality points (sale == target, margin == minimum, cost == sale) must
 * be pinned to avoid off-by-one drift.
 */
class ViabilityEvaluationServiceTests {

	private final ViabilityEvaluationService service = new ViabilityEvaluationService();

	private BudgetVersion version(BigDecimal targetAmount, BigDecimal totalCost, BigDecimal salePrice, BigDecimal margin) {
		BudgetVersion v = new BudgetVersion(UUID.randomUUID(), UUID.randomUUID(), 1);
		v.configureFinancials(targetAmount, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, UUID.randomUUID(), LocalDate.now());
		v.recordCalculation(totalCost, salePrice, margin, ViabilityStatus.VIABLE);
		return v;
	}

	@Test
	void salePriceEqualToTargetIsViable() {
		// salePrice == target is the limit; only salePrice > target is NO_VIABLE.
		BudgetVersion v = version(new BigDecimal("1000.00"), new BigDecimal("800.00"), new BigDecimal("1000.00"), new BigDecimal("0.2000"));
		assertThat(service.evaluateViability(v, List.of(), new BigDecimal("0.1000"))).isEqualTo(ViabilityStatus.VIABLE);
	}

	@Test
	void salePriceAboveTargetIsNotViable() {
		BudgetVersion v = version(new BigDecimal("1000.00"), new BigDecimal("800.00"), new BigDecimal("1000.01"), new BigDecimal("0.2000"));
		assertThat(service.evaluateViability(v, List.of(), new BigDecimal("0.1000"))).isEqualTo(ViabilityStatus.NO_VIABLE);
	}

	@Test
	void marginEqualToMinimumIsViable() {
		// margin == minimum is the limit; only margin < minimum is NO_VIABLE.
		BudgetVersion v = version(new BigDecimal("2000.00"), new BigDecimal("800.00"), new BigDecimal("1000.00"), new BigDecimal("0.1000"));
		assertThat(service.evaluateViability(v, List.of(), new BigDecimal("0.1000"))).isEqualTo(ViabilityStatus.VIABLE);
	}

	@Test
	void marginBelowMinimumIsNotViable() {
		BudgetVersion v = version(new BigDecimal("2000.00"), new BigDecimal("800.00"), new BigDecimal("1000.00"), new BigDecimal("0.0999"));
		assertThat(service.evaluateViability(v, List.of(), new BigDecimal("0.1000"))).isEqualTo(ViabilityStatus.NO_VIABLE);
	}

	@Test
	void costEqualToSalePriceIsNotViable() {
		// totalCost >= salePrice (with salePrice > 0) is NO_VIABLE at the equality limit.
		BudgetVersion v = version(new BigDecimal("2000.00"), new BigDecimal("1000.00"), new BigDecimal("1000.00"), BigDecimal.ZERO);
		assertThat(service.evaluateViability(v, List.of(), BigDecimal.ZERO)).isEqualTo(ViabilityStatus.NO_VIABLE);
	}

	@Test
	void zeroSalePriceIsNotPenalizedByCostComparison() {
		// Edge: a not-yet-calculated version (salePrice 0) is not forced to NO_VIABLE by the cost>=sale rule.
		BudgetVersion v = version(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
		assertThat(service.evaluateViability(v, List.of(), BigDecimal.ZERO)).isEqualTo(ViabilityStatus.VIABLE);
	}
}
