package com.backset.forze.module.document.application;

public interface DocumentRenderer {

	String renderHtml(TechnicalSmokeDocument document);

	byte[] renderPdf(TechnicalSmokeDocument document);
}
