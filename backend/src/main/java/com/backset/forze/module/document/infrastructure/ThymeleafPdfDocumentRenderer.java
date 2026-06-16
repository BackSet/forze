package com.backset.forze.module.document.infrastructure;

import java.io.ByteArrayOutputStream;

import com.backset.forze.module.document.application.DocumentRenderer;
import com.backset.forze.module.document.application.TechnicalSmokeDocument;
import com.backset.forze.module.document.domain.DocumentRenderException;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Component
class ThymeleafPdfDocumentRenderer implements DocumentRenderer {

	private final TemplateEngine templateEngine;

	ThymeleafPdfDocumentRenderer(TemplateEngine templateEngine) {
		this.templateEngine = templateEngine;
	}

	@Override
	public String renderHtml(TechnicalSmokeDocument document) {
		Context context = new Context();
		context.setVariable("document", document);
		return templateEngine.process("technical-smoke", context);
	}

	@Override
	public byte[] renderPdf(TechnicalSmokeDocument document) {
		try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			new PdfRendererBuilder()
					.withHtmlContent(renderHtml(document), null)
					.toStream(output)
					.run();
			return output.toByteArray();
		}
		catch (Exception exception) {
			throw new DocumentRenderException("Unable to render technical PDF.", exception);
		}
	}
}
