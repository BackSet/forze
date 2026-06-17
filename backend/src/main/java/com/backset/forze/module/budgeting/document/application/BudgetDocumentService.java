package com.backset.forze.module.budgeting.document.application;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.backset.forze.module.budgeting.domain.budget.Budget;
import com.backset.forze.module.budgeting.domain.budget.BudgetItem;
import com.backset.forze.module.budgeting.domain.budget.BudgetVersion;
import com.backset.forze.module.budgeting.domain.budget.Chapter;
import com.backset.forze.module.budgeting.domain.document.BudgetDocument;
import com.backset.forze.module.budgeting.domain.document.DocumentFormat;
import com.backset.forze.module.budgeting.domain.document.DocumentType;
import com.backset.forze.module.budgeting.domain.project.Client;
import com.backset.forze.module.budgeting.domain.project.Project;
import com.backset.forze.module.budgeting.infrastructure.BudgetDocumentRepository;
import com.backset.forze.module.budgeting.infrastructure.BudgetItemRepository;
import com.backset.forze.module.budgeting.infrastructure.BudgetRepository;
import com.backset.forze.module.budgeting.infrastructure.BudgetVersionRepository;
import com.backset.forze.module.budgeting.infrastructure.ChapterRepository;
import com.backset.forze.module.budgeting.infrastructure.ClientRepository;
import com.backset.forze.module.budgeting.infrastructure.OrganizationRepository;
import com.backset.forze.module.budgeting.infrastructure.ProjectRepository;
import com.backset.forze.module.budgeting.infrastructure.UnitOfMeasureRepository;
import com.backset.forze.shared.api.ApiException;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class BudgetDocumentService {

	private final BudgetDocumentRepository documentRepository;
	private final BudgetVersionRepository versionRepository;
	private final BudgetRepository budgetRepository;
	private final ProjectRepository projectRepository;
	private final ClientRepository clientRepository;
	private final OrganizationRepository organizationRepository;
	private final ChapterRepository chapterRepository;
	private final BudgetItemRepository itemRepository;
	private final UnitOfMeasureRepository unitRepository;
	private final TemplateEngine templateEngine;

	public BudgetDocumentService(
			BudgetDocumentRepository documentRepository,
			BudgetVersionRepository versionRepository,
			BudgetRepository budgetRepository,
			ProjectRepository projectRepository,
			ClientRepository clientRepository,
			OrganizationRepository organizationRepository,
			ChapterRepository chapterRepository,
			BudgetItemRepository itemRepository,
			UnitOfMeasureRepository unitRepository,
			TemplateEngine templateEngine
	) {
		this.documentRepository = documentRepository;
		this.versionRepository = versionRepository;
		this.budgetRepository = budgetRepository;
		this.projectRepository = projectRepository;
		this.clientRepository = clientRepository;
		this.organizationRepository = organizationRepository;
		this.chapterRepository = chapterRepository;
		this.itemRepository = itemRepository;
		this.unitRepository = unitRepository;
		this.templateEngine = templateEngine;
	}

	@Transactional(readOnly = true)
	public List<BudgetDocument> getDocuments(UUID versionId) {
		return documentRepository.findByBudgetVersionId(versionId);
	}

	@Transactional
	public byte[] generatePdf(UUID orgId, UUID versionId, DocumentType type, UUID userId) {
		BudgetVersion version = versionRepository.findById(versionId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Version de presupuesto no encontrada."));

		Budget budget = budgetRepository.findById(version.budgetId())
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Presupuesto no encontrado."));

		Project project = projectRepository.findById(budget.projectId())
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Proyecto no encontrado."));

		String clientName = "Consumidor Final";
		if (project.clientId() != null) {
			Client client = clientRepository.findById(project.clientId()).orElse(null);
			if (client != null) {
				clientName = client.name();
			}
		}

		var organization = organizationRepository.findById(orgId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Organizacion no encontrada."));

		// Load items and chapters
		List<Chapter> chapters = chapterRepository.findByBudgetVersionIdOrderByPosition(versionId);
		List<BudgetItem> items = itemRepository.findByBudgetVersionIdOrderByPosition(versionId);

		// Format data safely (no internal costs, yields, suppliers, margins)
		List<ChapterReport> chapterReports = new ArrayList<>();
		Map<UUID, List<ItemReport>> chapterItems = new HashMap<>();

		for (BudgetItem item : items) {
			String unit = "U";
			var uom = unitRepository.findById(item.unitId()).orElse(null);
			if (uom != null) {
				unit = uom.code();
			}

			ItemReport rItem = new ItemReport(
					item.code(),
					item.name(),
					item.description(),
					unit,
					item.quantity(),
					item.unitPrice(),
					item.totalSale()
			);

			UUID chapId = item.chapterId();
			chapterItems.computeIfAbsent(chapId, k -> new ArrayList<>()).add(rItem);
		}

		for (Chapter chap : chapters) {
			List<ItemReport> cItems = chapterItems.getOrDefault(chap.id(), List.of());
			chapterReports.add(new ChapterReport(chap.code(), chap.name(), cItems));
		}

		// Items without chapter
		List<ItemReport> rootItems = chapterItems.getOrDefault(null, List.of());

		Context context = new Context();
		context.setVariable("orgName", organization.name());
		context.setVariable("projectName", project.name());
		context.setVariable("clientName", clientName);
		context.setVariable("location", project.location());
		context.setVariable("currency", budget.currencyCode());
		context.setVariable("totalAmount", version.salePrice());
		context.setVariable("chapters", chapterReports);
		context.setVariable("rootItems", rootItems);

		String templateName;
		if (type == DocumentType.COTIZACION) {
			templateName = "cotizacion";
		}
		else if (type == DocumentType.PRESUPUESTO_DETALLADO) {
			templateName = "presupuesto-detallado";
		}
		else {
			templateName = "resumen-capitulos";
		}

		String html = templateEngine.process(templateName, context);

		byte[] pdfBytes;
		try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			new PdfRendererBuilder()
					.withHtmlContent(html, null)
					.toStream(output)
					.run();
			pdfBytes = output.toByteArray();
		}
		catch (Exception e) {
			throw new RuntimeException("Error rendering document to PDF", e);
		}

		// Register the document in db
		String docNum = "DOC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
		BudgetDocument doc = new BudgetDocument(UUID.randomUUID(), orgId, versionId, type, DocumentFormat.PDF);
		doc.withMetadata(docNum, version.validUntil(), "Generado automaticamente", userId);
		documentRepository.save(doc);

		return pdfBytes;
	}

	public record ChapterReport(String code, String name, List<ItemReport> items) {}

	public record ItemReport(
			String code,
			String name,
			String description,
			String unit,
			BigDecimal quantity,
			BigDecimal unitPrice,
			BigDecimal totalSale
	) {}
}
