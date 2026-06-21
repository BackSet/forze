package com.backset.forze.module.budgeting.approval.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.backset.forze.module.budgeting.audit.application.AuditService;
import com.backset.forze.module.budgeting.domain.approval.ApprovalComment;
import com.backset.forze.module.budgeting.domain.approval.ApprovalRequest;
import com.backset.forze.module.budgeting.domain.budget.BudgetStatus;
import com.backset.forze.module.budgeting.domain.budget.BudgetVersion;
import com.backset.forze.module.budgeting.domain.budget.ViabilityStatus;
import com.backset.forze.module.budgeting.infrastructure.ApprovalCommentRepository;
import com.backset.forze.module.budgeting.infrastructure.ApprovalRequestRepository;
import com.backset.forze.module.budgeting.infrastructure.BudgetVersionRepository;
import com.backset.forze.shared.api.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApprovalService {

	private final ApprovalRequestRepository requestRepository;
	private final ApprovalCommentRepository commentRepository;
	private final BudgetVersionRepository versionRepository;
	private final AuditService auditService;

	public ApprovalService(
			ApprovalRequestRepository requestRepository,
			ApprovalCommentRepository commentRepository,
			BudgetVersionRepository versionRepository,
			AuditService auditService
	) {
		this.requestRepository = requestRepository;
		this.commentRepository = commentRepository;
		this.versionRepository = versionRepository;
		this.auditService = auditService;
	}

	@Transactional(readOnly = true)
	public List<ApprovalRequest> getRequestsForVersion(UUID versionId) {
		return requestRepository.findByBudgetVersionId(versionId);
	}

	@Transactional(readOnly = true)
	public List<ApprovalComment> getCommentsForRequest(UUID requestId) {
		return commentRepository.findByApprovalRequestId(requestId);
	}

	@Transactional
	public ApprovalRequest submitForApproval(UUID orgId, UUID versionId, UUID userId) {
		BudgetVersion version = versionRepository.findById(versionId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Version de presupuesto no encontrada."));

		if (version.status() == BudgetStatus.APROBADO) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "La version ya esta aprobada.");
		}

		if (version.viabilityStatus() == ViabilityStatus.NO_VIABLE) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "No se puede enviar a aprobacion una version NO_VIABLE.");
		}

		String previousStatus = version.status().name();
		version.changeStatus(BudgetStatus.PENDIENTE_APROBACION);
		versionRepository.save(version);

		ApprovalRequest request = new ApprovalRequest(UUID.randomUUID(), versionId, userId, Instant.now());
		ApprovalRequest saved = requestRepository.save(request);
		auditService.log(orgId, userId, "SUBMIT_APPROVAL", "BudgetVersion", versionId,
				previousStatus, BudgetStatus.PENDIENTE_APROBACION.name(), "Version enviada a aprobacion", null);
		return saved;
	}

	@Transactional
	public ApprovalRequest approve(UUID orgId, UUID requestId, UUID userId) {
		ApprovalRequest req = requestRepository.findById(requestId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Solicitud de aprobacion no encontrada."));

		BudgetVersion version = versionRepository.findById(req.budgetVersionId())
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Version de presupuesto no encontrada."));

		String previousStatus = version.status().name();
		req.approve(userId, Instant.now());
		version.approve(userId, Instant.now());

		versionRepository.save(version);
		ApprovalRequest saved = requestRepository.save(req);
		auditService.log(orgId, userId, "APPROVE", "BudgetVersion", version.id(),
				previousStatus, BudgetStatus.APROBADO.name(), "Version aprobada", null);
		return saved;
	}

	@Transactional
	public ApprovalRequest observe(UUID orgId, UUID requestId, UUID userId, String commentText) {
		if (commentText == null || commentText.isBlank()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Se requiere un comentario para observar la solicitud.");
		}

		ApprovalRequest req = requestRepository.findById(requestId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Solicitud de aprobacion no encontrada."));

		BudgetVersion version = versionRepository.findById(req.budgetVersionId())
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Version de presupuesto no encontrada."));

		String previousStatus = version.status().name();
		req.observe(userId, Instant.now());
		version.changeStatus(BudgetStatus.REQUIERE_AJUSTES);

		versionRepository.save(version);
		requestRepository.save(req);

		ApprovalComment comment = new ApprovalComment(UUID.randomUUID(), requestId, userId, commentText);
		commentRepository.save(comment);

		auditService.log(orgId, userId, "OBSERVE", "BudgetVersion", version.id(),
				previousStatus, BudgetStatus.REQUIERE_AJUSTES.name(), "Version observada", null);
		return req;
	}

	@Transactional
	public ApprovalRequest reject(UUID orgId, UUID requestId, UUID userId, String commentText) {
		if (commentText == null || commentText.isBlank()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Se requiere un comentario para rechazar la solicitud.");
		}

		ApprovalRequest req = requestRepository.findById(requestId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Solicitud de aprobacion no encontrada."));

		BudgetVersion version = versionRepository.findById(req.budgetVersionId())
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Version de presupuesto no encontrada."));

		String previousStatus = version.status().name();
		req.reject(userId, Instant.now());
		version.changeStatus(BudgetStatus.RECHAZADO);

		versionRepository.save(version);
		requestRepository.save(req);

		ApprovalComment comment = new ApprovalComment(UUID.randomUUID(), requestId, userId, commentText);
		commentRepository.save(comment);

		auditService.log(orgId, userId, "REJECT", "BudgetVersion", version.id(),
				previousStatus, BudgetStatus.RECHAZADO.name(), "Version rechazada", null);
		return req;
	}
}
