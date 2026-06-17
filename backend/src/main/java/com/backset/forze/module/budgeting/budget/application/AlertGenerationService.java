package com.backset.forze.module.budgeting.budget.application;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.backset.forze.module.budgeting.domain.budget.BudgetItem;
import com.backset.forze.module.budgeting.domain.budget.BudgetVersion;
import com.backset.forze.module.budgeting.domain.budget.ItemValidationStatus;
import org.springframework.stereotype.Service;

@Service
public class AlertGenerationService {

	public List<BudgetAlert> generateAlerts(
			BudgetVersion version,
			List<BudgetItem> items,
			BigDecimal minimumMargin
	) {
		List<BudgetAlert> alerts = new ArrayList<>();
		BigDecimal totalCost = version.totalCost() != null ? version.totalCost() : BigDecimal.ZERO;
		BigDecimal salePrice = version.salePrice() != null ? version.salePrice() : BigDecimal.ZERO;
		BigDecimal margin = version.margin() != null ? version.margin() : BigDecimal.ZERO;
		BigDecimal targetAmount = version.targetAmount();

		if (totalCost.compareTo(salePrice) >= 0 && salePrice.compareTo(BigDecimal.ZERO) > 0) {
			alerts.add(new BudgetAlert("totalCost", "El costo interno es mayor o igual al precio ofertado."));
		}

		if (targetAmount != null && targetAmount.compareTo(BigDecimal.ZERO) > 0) {
			if (salePrice.compareTo(targetAmount) > 0) {
				alerts.add(new BudgetAlert("salePrice", "El precio ofertado supera el monto objetivo."));
			}
		}

		if (minimumMargin != null && margin.compareTo(minimumMargin) < 0) {
			alerts.add(new BudgetAlert("margin", "El margen es inferior al minimo requerido (" + minimumMargin.multiply(new BigDecimal("100")).setScale(2) + "%)."));
		}

		for (BudgetItem item : items) {
			if (item.quantity() == null || item.quantity().compareTo(BigDecimal.ZERO) == 0) {
				alerts.add(new BudgetAlert("item:" + item.id(), "El rubro '" + item.name() + "' tiene cantidad cero."));
			}
			if (item.validationStatus() == ItemValidationStatus.INCOMPLETO) {
				alerts.add(new BudgetAlert("item:" + item.id(), "El rubro '" + item.name() + "' esta incompleto."));
			}
		}

		return alerts;
	}

	public record BudgetAlert(String field, String message) {}
}
