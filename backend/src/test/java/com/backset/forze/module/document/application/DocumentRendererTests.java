package com.backset.forze.module.document.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import com.backset.forze.TestBudgetingExcludedConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
		classes = TestBudgetingExcludedConfiguration.class,
		properties = {
				"debug=false",
				"forze.identity.enabled=false",
				"spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration"
		})
class DocumentRendererTests {

	@Autowired
	private DocumentRenderer renderer;

	@Test
	void rendersHtmlAndPdfSmokeDocument() {
		TechnicalSmokeDocument document = new TechnicalSmokeDocument("FORZE technical smoke", "Renderer verification.", Instant.parse("2026-06-16T18:00:00Z"));

		String html = renderer.renderHtml(document);
		byte[] pdf = renderer.renderPdf(document);

		assertThat(html).contains("FORZE technical smoke");
		assertThat(pdf).isNotEmpty();
		assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
	}
}
