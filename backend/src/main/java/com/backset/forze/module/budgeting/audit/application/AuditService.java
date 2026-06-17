package com.backset.forze.module.budgeting.audit.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.backset.forze.module.budgeting.domain.audit.AuditLogEntry;
import com.backset.forze.module.budgeting.infrastructure.AuditLogEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

	private final AuditLogEntryRepository auditLogEntryRepository;

	public AuditService(AuditLogEntryRepository auditLogEntryRepository) {
		this.auditLogEntryRepository = auditLogEntryRepository;
	}

	@Transactional
	public void log(UUID orgId, UUID userId, String action, String entityType, UUID entityId,
			String oldValue, String newValue, String reason, String ipAddress) {
		AuditLogEntry entry = new AuditLogEntry(
				UUID.randomUUID(),
				orgId,
				userId,
				action,
				entityType,
				entityId,
				Instant.now()
		);
		entry.withChange(oldValue, newValue, reason, ipAddress);
		auditLogEntryRepository.save(entry);
	}

	@Transactional(readOnly = true)
	public List<AuditLogEntry> getLogs(UUID orgId) {
		return auditLogEntryRepository.findByOrganizationIdOrderByOccurredAtDesc(orgId);
	}
}
