package com.backset.forze.module.document.application;

import java.time.Instant;

public record TechnicalSmokeDocument(String title, String description, Instant generatedAt) {
}
