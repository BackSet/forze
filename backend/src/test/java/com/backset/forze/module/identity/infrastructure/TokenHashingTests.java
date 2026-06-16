package com.backset.forze.module.identity.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TokenHashingTests {

	private final TokenHashing tokenHashing = new TokenHashing();

	@Test
	void hashesRefreshTokenWithoutReturningRawValue() {
		String hash = tokenHashing.sha256("raw-refresh-token");

		assertThat(hash).hasSize(64);
		assertThat(hash).doesNotContain("raw-refresh-token");
		assertThat(hash).isEqualTo(tokenHashing.sha256("raw-refresh-token"));
	}
}
