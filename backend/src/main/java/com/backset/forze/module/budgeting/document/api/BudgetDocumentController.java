package com.backset.forze.module.budgeting.document.api;

import java.util.List;
import java.util.UUID;

import com.backset.forze.module.budgeting.document.application.BudgetDocumentService;
import com.backset.forze.module.budgeting.domain.document.BudgetDocument;
import com.backset.forze.module.budgeting.domain.document.DocumentType;
import com.backset.forze.module.identity.infrastructure.UserPrincipal;
import com.backset.forze.shared.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class BudgetDocumentController {

	private final BudgetDocumentService documentService;

	public BudgetDocumentController(BudgetDocumentService documentService) {
		this.documentService = documentService;
	}

	@GetMapping("/budget-versions/{versionId}/documents")
	@Operation(summary = "List all generated documents for a budget version.")
	@PreAuthorize("@securityService.hasPermission('DOCUMENTOS_READ')")
	public List<DocumentDto> getDocuments(@PathVariable UUID versionId) {
		return documentService.getDocuments(versionId).stream()
				.map(d -> new DocumentDto(d.id(), d.budgetVersionId(), d.type().name(), d.format().name(), d.number()))
				.toList();
	}

	@PostMapping("/budget-versions/{versionId}/documents")
	@Operation(summary = "Generate a PDF document for a budget version.")
	@PreAuthorize("@securityService.hasPermission('DOCUMENTOS_WRITE')")
	public ResponseEntity<byte[]> generateDocument(
			@PathVariable UUID versionId,
			@RequestParam DocumentType type,
			@AuthenticationPrincipal UserPrincipal principal
	) {
		UUID orgId = TenantContext.getRequiredTenantId();
		byte[] pdfBytes = documentService.generatePdf(orgId, versionId, type, principal.id());

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_PDF);
		String filename = type.name().toLowerCase() + "-" + versionId + ".pdf";
		headers.setContentDispositionFormData("attachment", filename);

		return ResponseEntity.ok()
				.headers(headers)
				.body(pdfBytes);
	}

	public record DocumentDto(
			UUID id,
			UUID budgetVersionId,
			String type,
			String format,
			String number
	) {}
}
