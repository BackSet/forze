package com.backset.forze.module.identity.application;

import java.time.Instant;

public record AuthTokens(String accessToken, String refreshToken, Instant refreshExpiresAt) {
}
