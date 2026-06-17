package com.backset.forze.module.budgeting.project.application;

import java.util.List;
import java.util.UUID;

import com.backset.forze.module.budgeting.domain.project.Client;
import com.backset.forze.module.budgeting.infrastructure.ClientRepository;
import com.backset.forze.shared.api.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClientService {

	private final ClientRepository clientRepository;

	public ClientService(ClientRepository clientRepository) {
		this.clientRepository = clientRepository;
	}

	@Transactional(readOnly = true)
	public List<Client> getClients(UUID organizationId) {
		return clientRepository.findByOrganizationId(organizationId);
	}

	@Transactional
	public Client createClient(UUID organizationId, String name) {
		if (clientRepository.findByOrganizationId(organizationId).stream()
				.anyMatch(c -> name.equalsIgnoreCase(c.name()))) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Ya existe un cliente con ese nombre.");
		}

		Client client = new Client(UUID.randomUUID(), organizationId, name);
		return clientRepository.save(client);
	}

	@Transactional
	public Client updateClient(UUID organizationId, UUID clientId, String name) {
		Client client = clientRepository.findById(clientId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Cliente no encontrado."));

		if (!client.organizationId().equals(organizationId)) {
			throw new ApiException(HttpStatus.FORBIDDEN, "El cliente no pertenece a la organizacion activa.");
		}

		if (clientRepository.findByOrganizationId(organizationId).stream()
				.anyMatch(c -> !c.id().equals(clientId) && name.equalsIgnoreCase(c.name()))) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Ya existe otro cliente con ese nombre.");
		}

		client.rename(name);
		return clientRepository.save(client);
	}
}
