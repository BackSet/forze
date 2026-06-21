package com.backset.forze.module.budgeting.approval.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.backset.forze.module.budgeting.audit.application.AuditService;
import com.backset.forze.module.budgeting.domain.approval.ApprovalRequest;
import com.backset.forze.module.budgeting.domain.budget.BudgetVersion;
import com.backset.forze.module.budgeting.domain.budget.BudgetStatus;
import com.backset.forze.module.budgeting.domain.budget.ViabilityStatus;
import com.backset.forze.module.budgeting.infrastructure.ApprovalCommentRepository;
import com.backset.forze.module.budgeting.infrastructure.ApprovalRequestRepository;
import com.backset.forze.module.budgeting.infrastructure.BudgetVersionRepository;
import com.backset.forze.shared.api.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The approval lifecycle records critical commercial events to the audit trail
 * and refuses to send a NO_VIABLE version. Backend stays the authority.
 */
class ApprovalServiceTests {

	private ApprovalRequestRepository requestRepository;
	private ApprovalCommentRepository commentRepository;
	private BudgetVersionRepository versionRepository;
	private AuditService auditService;
	private ApprovalService service;

	private final UUID orgId = UUID.randomUUID();
	private final UUID userId = UUID.randomUUID();
	private final UUID versionId = UUID.randomUUID();

	@BeforeEach
	void setUp() {
		requestRepository = mock(ApprovalRequestRepository.class);
		commentRepository = mock(ApprovalCommentRepository.class);
		versionRepository = mock(BudgetVersionRepository.class);
		auditService = mock(AuditService.class);
		service = new ApprovalService(requestRepository, commentRepository, versionRepository, auditService);
		when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
		when(versionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
		when(commentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
	}

	private BudgetVersion draftVersion() {
		return new BudgetVersion(versionId, UUID.randomUUID(), 1); // BORRADOR, viability null
	}

	@Test
	void submittingAuditsTheEvent() {
		when(versionRepository.findById(versionId)).thenReturn(Optional.of(draftVersion()));

		service.submitForApproval(orgId, versionId, userId);

		verify(auditService).log(eq(orgId), eq(userId), eq("SUBMIT_APPROVAL"), eq("BudgetVersion"), eq(versionId),
				eq("BORRADOR"), eq(BudgetStatus.PENDIENTE_APROBACION.name()), any(), isNull());
	}

	@Test
	void cannotSubmitNonViableVersion() {
		BudgetVersion v = draftVersion();
		v.recordCalculation(null, null, null, ViabilityStatus.NO_VIABLE);
		when(versionRepository.findById(versionId)).thenReturn(Optional.of(v));

		assertThatThrownBy(() -> service.submitForApproval(orgId, versionId, userId))
				.isInstanceOf(ApiException.class)
				.hasMessageContaining("NO_VIABLE");
		verify(auditService, never()).log(any(), any(), any(), any(), any(), any(), any(), any(), any());
	}

	@Test
	void approvingAuditsAndApprovesVersion() {
		UUID requestId = UUID.randomUUID();
		ApprovalRequest req = new ApprovalRequest(requestId, versionId, userId, Instant.now());
		when(requestRepository.findById(requestId)).thenReturn(Optional.of(req));
		when(versionRepository.findById(versionId)).thenReturn(Optional.of(draftVersion()));

		service.approve(orgId, requestId, userId);

		verify(auditService).log(eq(orgId), eq(userId), eq("APPROVE"), eq("BudgetVersion"), eq(versionId),
				any(), eq(BudgetStatus.APROBADO.name()), any(), isNull());
	}

	@Test
	void rejectingAuditsTheEvent() {
		UUID requestId = UUID.randomUUID();
		ApprovalRequest req = new ApprovalRequest(requestId, versionId, userId, Instant.now());
		when(requestRepository.findById(requestId)).thenReturn(Optional.of(req));
		when(versionRepository.findById(versionId)).thenReturn(Optional.of(draftVersion()));

		service.reject(orgId, requestId, userId, "Falta documentación");

		verify(auditService).log(eq(orgId), eq(userId), eq("REJECT"), eq("BudgetVersion"), eq(versionId),
				any(), eq(BudgetStatus.RECHAZADO.name()), any(), isNull());
	}
}
