package com.backset.forze.module.budgeting.budget.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.backset.forze.module.budgeting.audit.application.AuditService;
import com.backset.forze.module.budgeting.catalog.application.CatalogService;
import com.backset.forze.module.budgeting.domain.budget.BudgetItem;
import com.backset.forze.module.budgeting.domain.budget.BudgetVersion;
import com.backset.forze.module.budgeting.domain.budget.Chapter;
import com.backset.forze.module.budgeting.infrastructure.BudgetItemRepository;
import com.backset.forze.module.budgeting.infrastructure.BudgetRepository;
import com.backset.forze.module.budgeting.infrastructure.BudgetRiskRepository;
import com.backset.forze.module.budgeting.infrastructure.BudgetVersionRepository;
import com.backset.forze.module.budgeting.infrastructure.ChapterRepository;
import com.backset.forze.module.budgeting.infrastructure.ItemApuComponentRepository;
import com.backset.forze.module.budgeting.infrastructure.ItemApuRepository;
import com.backset.forze.module.budgeting.infrastructure.MeasurementRepository;
import com.backset.forze.module.budgeting.infrastructure.PriceHistoryRepository;
import com.backset.forze.shared.api.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Chapters and budget items are mutated through the service layer (not the
 * BudgetVersion aggregate), so their immutability for an APROBADO version must
 * be enforced there. These tests pin that historical snapshots cannot change.
 */
class BudgetVersionImmutabilityTests {

	private BudgetVersionRepository versionRepository;
	private ChapterRepository chapterRepository;
	private BudgetItemRepository itemRepository;
	private BudgetService service;

	private final UUID orgId = UUID.randomUUID();
	private final UUID versionId = UUID.randomUUID();

	@BeforeEach
	void setUp() {
		versionRepository = mock(BudgetVersionRepository.class);
		chapterRepository = mock(ChapterRepository.class);
		itemRepository = mock(BudgetItemRepository.class);
		service = new BudgetService(
				mock(BudgetRepository.class),
				versionRepository,
				chapterRepository,
				itemRepository,
				mock(ItemApuRepository.class),
				mock(ItemApuComponentRepository.class),
				mock(MeasurementRepository.class),
				mock(CatalogService.class),
				mock(BudgetVersionCalculationService.class),
				mock(BudgetRiskRepository.class),
				mock(PriceHistoryRepository.class),
				mock(ApuCalculationService.class),
				mock(AuditService.class),
				mock(AlertGenerationService.class)
		);
	}

	// Real aggregate instances: a fresh version is BORRADOR; approving it flips
	// isApproved() to true. Avoids mocking entity methods.
	private BudgetVersion versionWithApproval(boolean approved) {
		BudgetVersion version = new BudgetVersion(versionId, UUID.randomUUID(), 1);
		if (approved) {
			version.approve(UUID.randomUUID(), Instant.now());
		}
		return version;
	}

	@Test
	void cannotAddChapterToApprovedVersion() {
		when(versionRepository.findById(versionId)).thenReturn(Optional.of(versionWithApproval(true)));

		assertThatThrownBy(() -> service.createChapter(versionId, "Capítulo 1", null))
				.isInstanceOf(ApiException.class)
				.hasMessageContaining("inmutable");
		verify(chapterRepository, never()).save(any());
	}

	@Test
	void canAddChapterToDraftVersion() {
		when(versionRepository.findById(versionId)).thenReturn(Optional.of(versionWithApproval(false)));
		when(chapterRepository.findByBudgetVersionIdOrderByPosition(versionId)).thenReturn(List.of());
		when(chapterRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		Chapter created = service.createChapter(versionId, "Capítulo 1", null);

		assertThat(created.name()).isEqualTo("Capítulo 1");
		verify(chapterRepository).save(any());
	}

	@Test
	void cannotChangeItemQuantityOnApprovedVersion() {
		UUID itemId = UUID.randomUUID();
		BudgetItem item = mock(BudgetItem.class);
		when(item.budgetVersionId()).thenReturn(versionId);
		when(itemRepository.findById(itemId)).thenReturn(Optional.of(item));
		when(versionRepository.findById(versionId)).thenReturn(Optional.of(versionWithApproval(true)));

		assertThatThrownBy(() -> service.updateItemQuantity(orgId, itemId, new BigDecimal("5")))
				.isInstanceOf(ApiException.class)
				.hasMessageContaining("inmutable");
		verify(itemRepository, never()).save(any());
	}
}
