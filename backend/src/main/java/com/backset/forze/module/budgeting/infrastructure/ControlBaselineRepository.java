package com.backset.forze.module.budgeting.infrastructure;

import java.util.Optional;
import java.util.UUID;

import com.backset.forze.module.budgeting.domain.control.ControlBaseline;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ControlBaselineRepository extends JpaRepository<ControlBaseline, UUID> {

	Optional<ControlBaseline> findByProjectId(UUID projectId);
}
