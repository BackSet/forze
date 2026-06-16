package com.backset.forze.module.identity.api;

import java.util.UUID;

public record MeResponse(UUID id, String username, String email) {
}
