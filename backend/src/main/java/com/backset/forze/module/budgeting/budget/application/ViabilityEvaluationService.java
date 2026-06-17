package com.backset.forze.module.budgeting.budget.application;

import java.math.BigDecimal;
import java.util.List;

import com.backset.forze.module.budgeting.domain.budget.BudgetItem;
import com.backset.forze.module.budgeting.domain.budget.BudgetVersion;
import com.backset.forze.module.budgeting.domain.budget.ItemValidationStatus;
import com.backset.forze.module.budgeting.domain.budget.ViabilityStatus;
import org.springframework.stereotype.Service;

@Service
public class ViabilityEvaluationService {

	public ViabilityStatus evaluateViability(
			BudgetVersion version,
			List<BudgetItem> items,
			BigDecimal minimumMargin
	) {
		BigDecimal totalCost = version.totalCost() != null ? version.totalCost() : BigDecimal.ZERO;
		BigDecimal salePrice = version.salePrice() != null ? version.salePrice() : BigDecimal.ZERO;
		BigDecimal margin = version.margin() != null ? version.margin() : BigDecimal.ZERO;
		BigDecimal targetAmount = version.targetAmount();

		if (totalCost.compareTo(salePrice) >= 0 && salePrice.compareTo(BigDecimal.ZERO) > 0) {
			return ViabilityStatus.NO_VIABLE;
		}

		if (targetAmount != null && targetAmount.compareTo(BigDecimal.ZERO) > 0) {
			if (salePrice.compareTo(targetAmount) > 0) {
				return ViabilityStatus.NO_VIABLE;
			}
		}

		if (minimumMargin != null && margin.compareTo(minimumMargin) < 0) {
			return ViabilityStatus.NO_VIABLE;
		}

		boolean hasIncomplete = false;
		for (BudgetItem item : items) {
			if (item.quantity() == null || item.quantity().compareTo(BigDecimal.ZERO) < 0) {
				return ViabilityStatus.NO_VIABLE;
			}
			if (item.validationStatus() == ItemValidationStatus.INCOMPLETO) {
				hasIncomplete = true;
			}
		}

		if (hasIncomplete) {
			return ViabilityStatus.VIABLE_CON_ALERTAS;
		}

		return ViabilityStatus.VIABLE;
	}
}
