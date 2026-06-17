package com.backset.forze.module.budgeting.infrastructure;

import java.util.Optional;
import java.util.UUID;

import com.backset.forze.module.budgeting.domain.admin.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionRepository extends JpaRepository<Permission, UUID> {

	Optional<Permission> findByCode(String code);
}
