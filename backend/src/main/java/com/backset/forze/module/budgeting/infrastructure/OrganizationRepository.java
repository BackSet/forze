package com.backset.forze.module.budgeting.infrastructure;

import java.util.UUID;

import com.backset.forze.module.budgeting.domain.admin.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {
}
