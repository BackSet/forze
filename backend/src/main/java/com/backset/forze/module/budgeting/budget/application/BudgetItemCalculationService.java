package com.backset.forze.module.budgeting.budget.application;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.backset.forze.module.budgeting.domain.budget.BudgetItem;
import org.springframework.stereotype.Service;

@Service
public class BudgetItemCalculationService {

	public void calculateItemTotals(BudgetItem item, BigDecimal indirectRate, BigDecimal contingencyRate, BigDecimal utilityRate) {
		BigDecimal qty = item.quantity() != null ? item.quantity() : BigDecimal.ZERO;
		BigDecimal unitCost = item.unitCost() != null ? item.unitCost() : BigDecimal.ZERO;

		BigDecimal totalCost = qty.multiply(unitCost).setScale(2, RoundingMode.HALF_UP);

		BigDecimal unitPrice = item.unitPrice();
		if (!item.priceLocked() || unitPrice == null) {
			BigDecimal sumRates = BigDecimal.ZERO;
			if (indirectRate != null) {
				sumRates = sumRates.add(indirectRate);
			}
			if (contingencyRate != null) {
				sumRates = sumRates.add(contingencyRate);
			}
			if (utilityRate != null) {
				sumRates = sumRates.add(utilityRate);
			}

			unitPrice = unitCost.multiply(BigDecimal.ONE.add(sumRates)).setScale(4, RoundingMode.HALF_UP);
		}

		BigDecimal totalSale = qty.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);

		BigDecimal margin = BigDecimal.ZERO;
		if (unitPrice.compareTo(BigDecimal.ZERO) > 0) {
			margin = unitPrice.subtract(unitCost).divide(unitPrice, 4, RoundingMode.HALF_UP);
		}

		item.recordPricing(unitCost, unitPrice, totalCost, totalSale, margin);
	}
}
