package com.backset.forze.module.budgeting.project.api;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.backset.forze.module.budgeting.domain.project.Client;
import com.backset.forze.module.budgeting.project.application.ClientService;
import com.backset.forze.shared.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clients")
public class ClientController {

	private final ClientService clientService;

	public ClientController(ClientService clientService) {
		this.clientService = clientService;
	}

	@GetMapping
	@Operation(summary = "List all clients for the active organization.")
	@PreAuthorize("@securityService.hasPermission('PROYECTOS_READ')")
	public List<ClientDto> listClients() {
		UUID orgId = TenantContext.getRequiredTenantId();
		return clientService.getClients(orgId).stream()
				.map(c -> new ClientDto(c.id(), c.organizationId(), c.name()))
				.toList();
	}

	@PostMapping
	@Operation(summary = "Create a new client in the active organization.")
	@PreAuthorize("@securityService.hasPermission('PROYECTOS_WRITE')")
	public ClientDto createClient(@Valid @RequestBody CreateClientRequest request) {
		UUID orgId = TenantContext.getRequiredTenantId();
		Client client = clientService.createClient(orgId, request.name());
		return new ClientDto(client.id(), client.organizationId(), client.name());
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update client name in the active organization.")
	@PreAuthorize("@securityService.hasPermission('PROYECTOS_WRITE')")
	public ClientDto updateClient(@PathVariable UUID id, @Valid @RequestBody CreateClientRequest request) {
		UUID orgId = TenantContext.getRequiredTenantId();
		Client client = clientService.updateClient(orgId, id, request.name());
		return new ClientDto(client.id(), client.organizationId(), client.name());
	}

	public record CreateClientRequest(
			@NotBlank @Size(min = 3, max = 200) String name
	) {}

	public record ClientDto(UUID id, UUID organizationId, String name) {}
}
