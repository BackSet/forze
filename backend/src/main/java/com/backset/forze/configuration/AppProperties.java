package com.backset.forze.configuration;

import java.time.ZoneId;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "forze")
public record AppProperties(ZoneId timeZone) {
}
