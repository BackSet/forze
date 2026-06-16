package com.backset.forze.module.budgeting.infrastructure;

import java.util.List;
import java.util.UUID;

import com.backset.forze.module.budgeting.domain.control.ProgressEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgressEntryRepository extends JpaRepository<ProgressEntry, UUID> {

	List<ProgressEntry> findByBaselineId(UUID baselineId);
}
