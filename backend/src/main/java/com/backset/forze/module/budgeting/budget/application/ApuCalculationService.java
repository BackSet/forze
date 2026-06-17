package com.backset.forze.module.budgeting.budget.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import com.backset.forze.module.budgeting.domain.budget.ItemApu;
import com.backset.forze.module.budgeting.domain.budget.ItemApuComponent;
import com.backset.forze.module.budgeting.domain.catalog.ComponentSection;
import org.springframework.stereotype.Service;

@Service
public class ApuCalculationService {

	public BigDecimal calculateComponentLineTotal(ItemApuComponent comp, BigDecimal apuYield) {
		BigDecimal qty = comp.quantity() != null ? comp.quantity() : BigDecimal.ZERO;
		BigDecimal price = comp.unitPrice() != null ? comp.unitPrice() : BigDecimal.ZERO;
		BigDecimal waste = comp.wasteFactor() != null ? comp.wasteFactor() : BigDecimal.ZERO;
		BigDecimal compYield = comp.yield();

		if (comp.section() == ComponentSection.MANO_DE_OBRA || comp.section() == ComponentSection.EQUIPOS) {
			BigDecimal effectiveYield = compYield != null && compYield.compareTo(BigDecimal.ZERO) > 0 ? compYield
					: (apuYield != null && apuYield.compareTo(BigDecimal.ZERO) > 0 ? apuYield : BigDecimal.ONE);
			return qty.multiply(price).multiply(BigDecimal.ONE.add(waste))
					.divide(effectiveYield, 4, RoundingMode.HALF_UP)
					.setScale(2, RoundingMode.HALF_UP);
		}
		else {
			return qty.multiply(price).multiply(BigDecimal.ONE.add(waste))
					.setScale(2, RoundingMode.HALF_UP);
		}
	}

	public BigDecimal calculateApuUnitCost(ItemApu apu, List<ItemApuComponent> components) {
		BigDecimal total = BigDecimal.ZERO;
		BigDecimal apuYield = apu.yield() != null && apu.yield().compareTo(BigDecimal.ZERO) > 0 ? apu.yield() : BigDecimal.ONE;

		for (ItemApuComponent comp : components) {
			BigDecimal lineTotal = calculateComponentLineTotal(comp, apuYield);
			total = total.add(lineTotal);
		}
		return total.setScale(4, RoundingMode.HALF_UP);
	}
}
