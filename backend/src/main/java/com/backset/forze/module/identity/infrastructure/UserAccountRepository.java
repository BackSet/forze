package com.backset.forze.module.identity.infrastructure;

import java.util.Optional;
import java.util.UUID;

import com.backset.forze.module.identity.domain.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {

	Optional<UserAccount> findByUsername(String username);

	boolean existsByUsername(String username);
}
