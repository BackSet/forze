package com.backset.forze.module.budgeting.infrastructure;

import java.util.List;
import java.util.UUID;

import com.backset.forze.module.budgeting.domain.audit.AuditLogEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogEntryRepository extends JpaRepository<AuditLogEntry, UUID> {

	List<AuditLogEntry> findByEntityTypeAndEntityId(String entityType, UUID entityId);

	List<AuditLogEntry> findByOrganizationIdOrderByOccurredAtDesc(UUID organizationId);
}
