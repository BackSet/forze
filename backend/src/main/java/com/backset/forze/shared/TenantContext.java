package com.backset.forze.shared;

import java.util.UUID;

public final class TenantContext {
	private static final ThreadLocal<UUID> CURRENT_TENANT = new ThreadLocal<>();

	private TenantContext() {
	}

	public static void setTenantId(UUID tenantId) {
		CURRENT_TENANT.set(tenantId);
	}

	public static UUID getTenantId() {
		return CURRENT_TENANT.get();
	}

	public static UUID getRequiredTenantId() {
		UUID id = CURRENT_TENANT.get();
		if (id == null) {
			throw new IllegalStateException("No active organization context found for this request.");
		}
		return id;
	}

	public static void clear() {
		CURRENT_TENANT.remove();
	}
}
