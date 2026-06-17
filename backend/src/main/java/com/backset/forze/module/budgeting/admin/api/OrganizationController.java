package com.backset.forze.module.budgeting.admin.api;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.backset.forze.module.budgeting.admin.application.OrganizationService;
import com.backset.forze.module.budgeting.domain.admin.Organization;
import com.backset.forze.module.identity.infrastructure.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/organizations")
public class OrganizationController {

	private final OrganizationService organizationService;

	public OrganizationController(OrganizationService organizationService) {
		this.organizationService = organizationService;
	}

	@PostMapping
	@Operation(summary = "Create a new organization.")
	public OrganizationDto createOrganization(
			@Valid @RequestBody CreateOrganizationRequest request,
			@AuthenticationPrincipal UserPrincipal principal
	) {
		Organization org = organizationService.createOrganization(request.name(), principal.id());
		return new OrganizationDto(org.id(), org.name());
	}

	@GetMapping
	@Operation(summary = "List all organizations the current user has access to.")
	public List<OrganizationDto> listOrganizations(@AuthenticationPrincipal UserPrincipal principal) {
		return organizationService.getUserOrganizations(principal.id()).stream()
				.map(org -> new OrganizationDto(org.id(), org.name()))
				.toList();
	}

	public record CreateOrganizationRequest(
			@NotBlank @Size(min = 3, max = 160) String name
	) {}

	public record OrganizationDto(UUID id, String name) {}
}
