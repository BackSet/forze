package com.backset.forze.module.budgeting.budget.application;

import java.math.BigDecimal;

import com.backset.forze.module.budgeting.domain.budget.BudgetItem;
import org.springframework.stereotype.Service;

@Service
public class BudgetItemCalculationService {

	public void calculateItemTotals(BudgetItem item, BigDecimal indirectRate, BigDecimal contingencyRate, BigDecimal utilityRate) {
		BigDecimal qty = item.quantity() != null ? item.quantity() : BigDecimal.ZERO;
		BigDecimal unitCost = item.unitCost() != null ? item.unitCost() : BigDecimal.ZERO;

		BigDecimal totalCost = BudgetRounding.money(qty.multiply(unitCost));

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

			unitPrice = BudgetRounding.unit(unitCost.multiply(BigDecimal.ONE.add(sumRates)));
		}

		BigDecimal totalSale = BudgetRounding.money(qty.multiply(unitPrice));

		BigDecimal margin = BigDecimal.ZERO;
		if (unitPrice.compareTo(BigDecimal.ZERO) > 0) {
			margin = BudgetRounding.divideUnit(unitPrice.subtract(unitCost), unitPrice);
		}

		item.recordPricing(unitCost, unitPrice, totalCost, totalSale, margin);
	}
}
