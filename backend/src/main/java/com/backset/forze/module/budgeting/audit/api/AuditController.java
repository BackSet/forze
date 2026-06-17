package com.backset.forze.module.budgeting.audit.api;

import java.util.List;
import java.util.UUID;

import com.backset.forze.module.budgeting.audit.application.AuditService;
import com.backset.forze.shared.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditController {

	private final AuditService auditService;

	public AuditController(AuditService auditService) {
		this.auditService = auditService;
	}

	@GetMapping
	@Operation(summary = "List audit log entries for the active organization.")
	@PreAuthorize("@securityService.hasPermission('AUDITORIA_READ')")
	public List<AuditLogDto> listLogs() {
		UUID orgId = TenantContext.getRequiredTenantId();
		return auditService.getLogs(orgId).stream()
				.map(log -> new AuditLogDto(
						log.id(),
						log.organizationId(),
						log.action(),
						log.entityType(),
						log.entityId(),
						log.occurredAt()
				))
				.toList();
	}

	public record AuditLogDto(
			UUID id,
			UUID organizationId,
			String action,
			String entityType,
			UUID entityId,
			java.time.Instant occurredAt
	) {}
}
