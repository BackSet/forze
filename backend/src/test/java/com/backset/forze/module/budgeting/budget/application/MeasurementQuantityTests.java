package com.backset.forze.module.budgeting.budget.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.backset.forze.module.budgeting.audit.application.AuditService;
import com.backset.forze.module.budgeting.catalog.application.CatalogService;
import com.backset.forze.module.budgeting.domain.budget.BudgetItem;
import com.backset.forze.module.budgeting.domain.budget.BudgetVersion;
import com.backset.forze.module.budgeting.domain.budget.Measurement;
import com.backset.forze.module.budgeting.infrastructure.BudgetItemRepository;
import com.backset.forze.module.budgeting.infrastructure.BudgetRepository;
import com.backset.forze.module.budgeting.infrastructure.BudgetRiskRepository;
import com.backset.forze.module.budgeting.infrastructure.BudgetVersionRepository;
import com.backset.forze.module.budgeting.infrastructure.ChapterRepository;
import com.backset.forze.module.budgeting.infrastructure.ItemApuComponentRepository;
import com.backset.forze.module.budgeting.infrastructure.ItemApuRepository;
import com.backset.forze.module.budgeting.infrastructure.MeasurementRepository;
import com.backset.forze.module.budgeting.infrastructure.PriceHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Structured measurements must feed the budget item quantity: the quantity is
 * the sum of the measurement results (the takeoff / computo metrico).
 */
class MeasurementQuantityTests {

	private BudgetVersionRepository versionRepository;
	private BudgetItemRepository itemRepository;
	private MeasurementRepository measurementRepository;
	private BudgetService service;

	private final UUID orgId = UUID.randomUUID();
	private final UUID versionId = UUID.randomUUID();
	private final UUID itemId = UUID.randomUUID();

	@BeforeEach
	void setUp() {
		versionRepository = mock(BudgetVersionRepository.class);
		itemRepository = mock(BudgetItemRepository.class);
		measurementRepository = mock(MeasurementRepository.class);
		service = new BudgetService(
				mock(BudgetRepository.class),
				versionRepository,
				mock(ChapterRepository.class),
				itemRepository,
				mock(ItemApuRepository.class),
				mock(ItemApuComponentRepository.class),
				measurementRepository,
				mock(CatalogService.class),
				mock(BudgetVersionCalculationService.class),
				mock(BudgetRiskRepository.class),
				mock(PriceHistoryRepository.class),
				mock(ApuCalculationService.class),
				mock(AuditService.class),
				mock(AlertGenerationService.class)
		);
	}

	private Measurement measurementWithResult(BigDecimal result) {
		Measurement m = new Measurement(UUID.randomUUID(), itemId, 1);
		m.recordResult("formula", result, null);
		return m;
	}

	@Test
	void addingMeasurementSetsItemQuantityToSumOfResults() {
		BudgetItem item = new BudgetItem(itemId, versionId, "Rubro", UUID.randomUUID(), BigDecimal.ZERO, 1);
		BudgetVersion draft = new BudgetVersion(versionId, UUID.randomUUID(), 1); // BORRADOR

		when(itemRepository.findById(itemId)).thenReturn(Optional.of(item));
		when(versionRepository.findById(versionId)).thenReturn(Optional.of(draft));
		// After saving, the item has two measurement lines: 12.50 + 7.50 = 20.00.
		when(measurementRepository.findByBudgetItemIdOrderByPosition(itemId))
				.thenReturn(List.of(measurementWithResult(new BigDecimal("12.50")), measurementWithResult(new BigDecimal("7.50"))));
		when(measurementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
		when(itemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		service.addMeasurement(orgId, itemId,
				new BudgetService.AddMeasurementCmd("muro", new BigDecimal("2.5"), BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, "2.5", null));

		assertThat(item.quantity()).isEqualByComparingTo("20.00");
	}
}
