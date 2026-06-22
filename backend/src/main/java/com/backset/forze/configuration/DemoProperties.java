package com.backset.forze.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the local/dev demo data seeder.
 *
 * <p>{@code password} is the shared password assigned to the fictitious demo
 * accounts. It is a local-only, non-secret convenience value; it is never
 * logged. When unset the seeder falls back to a built-in default.
 */
@ConfigurationProperties(prefix = "forze.demo")
public record DemoProperties(boolean enabled, String password) {
}
