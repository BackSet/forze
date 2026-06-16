package com.backset.forze.module.budgeting.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import com.backset.forze.module.budgeting.domain.budget.BudgetStatus;
import com.backset.forze.module.budgeting.domain.budget.BudgetVersion;
import org.junit.jupiter.api.Test;

/**
 * Domain invariants for {@link BudgetVersion} (design section 16: an approved version is immutable).
 * Pure unit test, runs without Docker.
 */
class BudgetVersionInvariantTests {

	@Test
	void approvedVersionRejectsFurtherEdits() {
		BudgetVersion version = new BudgetVersion(UUID.randomUUID(), UUID.randomUUID(), 1);
		version.approve(UUID.randomUUID(), Instant.parse("2026-06-16T18:00:00Z"));

		assertThat(version.isApproved()).isTrue();
		assertThat(version.status()).isEqualTo(BudgetStatus.APROBADO);
		assertThatThrownBy(() -> version.changeStatus(BudgetStatus.BORRADOR))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void draftVersionAllowsStatusTransition() {
		BudgetVersion version = new BudgetVersion(UUID.randomUUID(), UUID.randomUUID(), 1);

		version.changeStatus(BudgetStatus.PENDIENTE_APROBACION);

		assertThat(version.status()).isEqualTo(BudgetStatus.PENDIENTE_APROBACION);
	}
}
