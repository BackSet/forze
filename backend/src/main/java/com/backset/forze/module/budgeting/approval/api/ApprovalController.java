package com.backset.forze.module.budgeting.approval.api;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import com.backset.forze.module.budgeting.approval.application.ApprovalService;
import com.backset.forze.module.budgeting.domain.approval.ApprovalComment;
import com.backset.forze.module.budgeting.domain.approval.ApprovalRequest;
import com.backset.forze.module.identity.infrastructure.UserPrincipal;
import com.backset.forze.shared.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ApprovalController {

	private final ApprovalService approvalService;

	public ApprovalController(ApprovalService approvalService) {
		this.approvalService = approvalService;
	}

	@GetMapping("/budget-versions/{versionId}/approvals")
	@Operation(summary = "List all approval requests for a budget version.")
	@PreAuthorize("@securityService.hasPermission('APROBACIONES_READ')")
	public List<ApprovalRequestDto> getRequests(@PathVariable UUID versionId) {
		return approvalService.getRequestsForVersion(versionId).stream()
				.map(this::toDto)
				.toList();
	}

	@PostMapping("/budget-versions/{versionId}/approvals")
	@Operation(summary = "Submit a budget version for approval.")
	@PreAuthorize("@securityService.hasPermission('BUDGETS_WRITE')")
	public ApprovalRequestDto submit(
			@PathVariable UUID versionId,
			@AuthenticationPrincipal UserPrincipal principal
	) {
		UUID orgId = TenantContext.getRequiredTenantId();
		ApprovalRequest req = approvalService.submitForApproval(orgId, versionId, principal.id());
		return toDto(req);
	}

	@PutMapping("/approvals/{requestId}/approve")
	@Operation(summary = "Approve a budget version approval request.")
	@PreAuthorize("@securityService.hasPermission('APROBACIONES_WRITE')")
	public ApprovalRequestDto approve(
			@PathVariable UUID requestId,
			@AuthenticationPrincipal UserPrincipal principal
	) {
		UUID orgId = TenantContext.getRequiredTenantId();
		ApprovalRequest req = approvalService.approve(orgId, requestId, principal.id());
		return toDto(req);
	}

	@PutMapping("/approvals/{requestId}/observe")
	@Operation(summary = "Observe a budget version approval request, requiring adjustments.")
	@PreAuthorize("@securityService.hasPermission('APROBACIONES_WRITE')")
	public ApprovalRequestDto observe(
			@PathVariable UUID requestId,
			@Valid @RequestBody DecisionRequest request,
			@AuthenticationPrincipal UserPrincipal principal
	) {
		UUID orgId = TenantContext.getRequiredTenantId();
		ApprovalRequest req = approvalService.observe(orgId, requestId, principal.id(), request.comment());
		return toDto(req);
	}

	@PutMapping("/approvals/{requestId}/reject")
	@Operation(summary = "Reject a budget version approval request.")
	@PreAuthorize("@securityService.hasPermission('APROBACIONES_WRITE')")
	public ApprovalRequestDto reject(
			@PathVariable UUID requestId,
			@Valid @RequestBody DecisionRequest request,
			@AuthenticationPrincipal UserPrincipal principal
	) {
		UUID orgId = TenantContext.getRequiredTenantId();
		ApprovalRequest req = approvalService.reject(orgId, requestId, principal.id(), request.comment());
		return toDto(req);
	}

	@GetMapping("/approvals/{requestId}/comments")
	@Operation(summary = "List comments for an approval request.")
	@PreAuthorize("@securityService.hasPermission('APROBACIONES_READ')")
	public List<CommentDto> getComments(@PathVariable UUID requestId) {
		return approvalService.getCommentsForRequest(requestId).stream()
				.map(c -> new CommentDto(c.id(), c.approvalRequestId(), c.budgetItemId(), c.comment(), c.response()))
				.toList();
	}

	private ApprovalRequestDto toDto(ApprovalRequest r) {
		return new ApprovalRequestDto(
				r.id(),
				r.budgetVersionId(),
				r.status().name(),
				r.decidedByUserId(),
				r.decidedAt()
		);
	}

	public record DecisionRequest(
			@NotBlank String comment
	) {}

	public record ApprovalRequestDto(
			UUID id,
			UUID budgetVersionId,
			String status,
			UUID decidedByUserId,
			java.time.Instant decidedAt
	) {}

	public record CommentDto(
			UUID id,
			UUID approvalRequestId,
			UUID budgetItemId,
			String comment,
			String response
	) {}
}
