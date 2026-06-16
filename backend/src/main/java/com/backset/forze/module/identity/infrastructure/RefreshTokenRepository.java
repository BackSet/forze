package com.backset.forze.module.identity.infrastructure;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.backset.forze.module.identity.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

	Optional<RefreshToken> findByTokenHash(String tokenHash);

	List<RefreshToken> findByFamilyId(UUID familyId);

	List<RefreshToken> findByUser_IdAndRevokedAtIsNullAndExpiresAtAfter(UUID userId, Instant now);
}
