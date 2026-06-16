package com.backset.forze.module.identity.infrastructure;

import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.stereotype.Component;

@Component
public class RefreshTokenGenerator {

	private final SecureRandom secureRandom = new SecureRandom();

	public String generate() {
		byte[] token = new byte[64];
		secureRandom.nextBytes(token);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(token);
	}
}
