package com.backset.forze.module.identity.application;

import java.util.UUID;

public record AuthenticatedUser(UUID id, String username, String email) {
}
