package com.backset.forze.module.budgeting.budget.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.backset.forze.module.budgeting.domain.budget.BudgetItem;
import com.backset.forze.module.budgeting.domain.budget.BudgetVersion;
import com.backset.forze.module.budgeting.domain.budget.ItemApu;
import com.backset.forze.module.budgeting.domain.budget.ItemApuComponent;
import com.backset.forze.module.budgeting.domain.budget.ItemValidationStatus;
import com.backset.forze.module.budgeting.domain.budget.ViabilityStatus;
import com.backset.forze.module.budgeting.domain.catalog.ComponentSection;
import org.junit.jupiter.api.Test;

class BudgetCalculationServiceTests {

	private final ApuCalculationService apuCalc = new ApuCalculationService();
	private final BudgetItemCalculationService itemCalc = new BudgetItemCalculationService();
	private final ViabilityEvaluationService viabilityEval = new ViabilityEvaluationService();
	private final AlertGenerationService alertGen = new AlertGenerationService(
			mock(com.backset.forze.module.budgeting.infrastructure.ItemApuRepository.class),
			mock(com.backset.forze.module.budgeting.infrastructure.ItemApuComponentRepository.class),
			mock(com.backset.forze.module.budgeting.infrastructure.PriceHistoryRepository.class),
			mock(com.backset.forze.module.budgeting.infrastructure.BudgetRiskRepository.class),
			mock(com.backset.forze.module.budgeting.infrastructure.MeasurementRepository.class),
			mock(com.backset.forze.module.budgeting.infrastructure.ApuMaestroRepository.class)
	);

	@Test
	void testApuComponentLineTotal() {
		// Material component (no yield divide, direct multiplication: qty * price * (1 + waste))
		ItemApuComponent mat = mock(ItemApuComponent.class);
		when(mat.section()).thenReturn(ComponentSection.MATERIALES);
		when(mat.quantity()).thenReturn(new BigDecimal("10.0000"));
		when(mat.unitPrice()).thenReturn(new BigDecimal("5.5000"));
		when(mat.wasteFactor()).thenReturn(new BigDecimal("0.0500")); // 5% waste

		BigDecimal matTotal = apuCalc.calculateComponentLineTotal(mat, BigDecimal.ONE);
		// Expected: 10 * 5.5 * 1.05 = 57.75
		assertThat(matTotal).isEqualByComparingTo("57.75");

		// Labor component (divided by yield)
		ItemApuComponent labor = mock(ItemApuComponent.class);
		when(labor.section()).thenReturn(ComponentSection.MANO_DE_OBRA);
		when(labor.quantity()).thenReturn(new BigDecimal("2.0000"));
		when(labor.unitPrice()).thenReturn(new BigDecimal("15.0000"));
		when(labor.wasteFactor()).thenReturn(BigDecimal.ZERO);
		when(labor.yield()).thenReturn(new BigDecimal("5.0000")); // yield = 5

		BigDecimal laborTotal = apuCalc.calculateComponentLineTotal(labor, BigDecimal.ONE);
		// Expected: (2 * 15 * 1.0) / 5 = 6.00
		assertThat(laborTotal).isEqualByComparingTo("6.00");
	}

	@Test
	void testApuUnitCostRollup() {
		ItemApu apu = mock(ItemApu.class);
		when(apu.yield()).thenReturn(new BigDecimal("2.5000"));

		ItemApuComponent mat = mock(ItemApuComponent.class);
		when(mat.section()).thenReturn(ComponentSection.MATERIALES);
		when(mat.quantity()).thenReturn(new BigDecimal("1.0000"));
		when(mat.unitPrice()).thenReturn(new BigDecimal("10.0000"));
		when(mat.wasteFactor()).thenReturn(BigDecimal.ZERO);

		ItemApuComponent labor = mock(ItemApuComponent.class);
		when(labor.section()).thenReturn(ComponentSection.MANO_DE_OBRA);
		when(labor.quantity()).thenReturn(new BigDecimal("1.0000"));
		when(labor.unitPrice()).thenReturn(new BigDecimal("20.0000"));
		when(labor.wasteFactor()).thenReturn(BigDecimal.ZERO);
		when(labor.yield()).thenReturn(BigDecimal.ZERO); // should fallback to apu yield: 2.5

		BigDecimal unitCost = apuCalc.calculateApuUnitCost(apu, List.of(mat, labor));
		// mat: 1 * 10 * 1 = 10.00
		// labor: 1 * 20 * 1 / 2.5 = 8.00
		// Total: 18.0000
		assertThat(unitCost).isEqualByComparingTo("18.0000");
	}

	@Test
	void testBudgetItemTotalsAndMargins() {
		BudgetItem item = new BudgetItem(UUID.randomUUID(), UUID.randomUUID(), "Piso ceramico", UUID.randomUUID(), new BigDecimal("100.0000"), 1);
		item.recordPricing(new BigDecimal("15.0000"), null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

		// With indirect rate = 10%, utility = 5%, contingency = 5% (Total 20% markup)
		itemCalc.calculateItemTotals(item, new BigDecimal("0.1000"), new BigDecimal("0.0500"), new BigDecimal("0.0500"));

		// Expected unit price: 15 * 1.2 = 18.0000
		assertThat(item.unitPrice()).isEqualByComparingTo("18.0000");
		// Expected total cost: 100 * 15 = 1500.00
		assertThat(item.totalCost()).isEqualByComparingTo("1500.00");
		// Expected total sale: 100 * 18 = 1800.00
		assertThat(item.totalSale()).isEqualByComparingTo("1800.00");
		// Expected margin: (18 - 15) / 18 = 0.1667 (16.67%)
		assertThat(item.margin()).isEqualByComparingTo("0.1667");
	}

	@Test
	void testViabilityEvaluation() {
		BudgetVersion version = mock(BudgetVersion.class);
		when(version.totalCost()).thenReturn(new BigDecimal("1000.00"));
		when(version.salePrice()).thenReturn(new BigDecimal("1200.00"));
		when(version.margin()).thenReturn(new BigDecimal("0.1667"));
		when(version.targetAmount()).thenReturn(new BigDecimal("1300.00"));

		BudgetItem item = mock(BudgetItem.class);
		when(item.quantity()).thenReturn(new BigDecimal("10.0000"));
		when(item.validationStatus()).thenReturn(ItemValidationStatus.COMPLETO);

		// Viable case
		ViabilityStatus status = viabilityEval.evaluateViability(version, List.of(item), new BigDecimal("0.1000"));
		assertThat(status).isEqualTo(ViabilityStatus.VIABLE);

		// Marginally non-viable because margin (16.67%) < minimum margin (20.00%)
		status = viabilityEval.evaluateViability(version, List.of(item), new BigDecimal("0.2000"));
		assertThat(status).isEqualTo(ViabilityStatus.NO_VIABLE);

		// Non-viable because sale price exceeds target amount
		when(version.targetAmount()).thenReturn(new BigDecimal("1100.00"));
		status = viabilityEval.evaluateViability(version, List.of(item), new BigDecimal("0.1000"));
		assertThat(status).isEqualTo(ViabilityStatus.NO_VIABLE);
	}

	@Test
	void testAlertGeneration() {
		BudgetVersion version = mock(BudgetVersion.class);
		when(version.totalCost()).thenReturn(new BigDecimal("1200.00")); // cost > sale
		when(version.salePrice()).thenReturn(new BigDecimal("1000.00"));
		when(version.margin()).thenReturn(new BigDecimal("-0.2000"));
		when(version.targetAmount()).thenReturn(new BigDecimal("900.00")); // sale > target

		BudgetItem item = mock(BudgetItem.class);
		when(item.id()).thenReturn(UUID.randomUUID());
		when(item.name()).thenReturn("Excavacion");
		when(item.quantity()).thenReturn(BigDecimal.ZERO);
		when(item.validationStatus()).thenReturn(ItemValidationStatus.INCOMPLETO);

		List<AlertGenerationService.BudgetAlert> alerts = alertGen.generateAlerts(version, List.of(item), new BigDecimal("0.1000"));
		assertThat(alerts).hasSize(6);

		assertThat(alerts.stream().anyMatch(a -> a.message().contains("costo interno es mayor"))).isTrue();
		assertThat(alerts.stream().anyMatch(a -> a.message().contains("supera el monto objetivo"))).isTrue();
		assertThat(alerts.stream().anyMatch(a -> a.message().contains("margen es inferior"))).isTrue();
		assertThat(alerts.stream().anyMatch(a -> a.message().contains("cantidad cero"))).isTrue();
		assertThat(alerts.stream().anyMatch(a -> a.message().contains("esta incompleto"))).isTrue();
		assertThat(alerts.stream().anyMatch(a -> a.message().contains("no tiene un APU configurado"))).isTrue();
	}
}
