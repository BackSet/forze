package com.backset.forze.module.budgeting.project.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.backset.forze.module.budgeting.domain.project.Project;
import com.backset.forze.module.budgeting.project.application.ProjectService;
import com.backset.forze.module.identity.domain.UserAccount;
import com.backset.forze.shared.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

	private final ProjectService projectService;

	public ProjectController(ProjectService projectService) {
		this.projectService = projectService;
	}

	@GetMapping
	@Operation(summary = "List all projects in the active organization.")
	@PreAuthorize("@securityService.hasPermission('PROYECTOS_READ')")
	public List<ProjectDto> listProjects() {
		UUID orgId = TenantContext.getRequiredTenantId();
		return projectService.getProjects(orgId).stream()
				.map(this::toDto)
				.toList();
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get project details by ID.")
	@PreAuthorize("@securityService.hasPermission('PROYECTOS_READ')")
	public ProjectDto getProject(@PathVariable UUID id) {
		UUID orgId = TenantContext.getRequiredTenantId();
		return toDto(projectService.getProject(orgId, id));
	}

	@PostMapping
	@Operation(summary = "Create a new project in the active organization.")
	@PreAuthorize("@securityService.hasPermission('PROYECTOS_WRITE')")
	public ProjectDto createProject(@Valid @RequestBody CreateProjectRequest request) {
		UUID orgId = TenantContext.getRequiredTenantId();
		Project project = projectService.createProject(orgId, toCmd(request));
		return toDto(project);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update an existing project in the active organization.")
	@PreAuthorize("@securityService.hasPermission('PROYECTOS_WRITE')")
	public ProjectDto updateProject(@PathVariable UUID id, @Valid @RequestBody CreateProjectRequest request) {
		UUID orgId = TenantContext.getRequiredTenantId();
		Project project = projectService.updateProject(orgId, id, toCmd(request));
		return toDto(project);
	}

	@PutMapping("/{id}/archive")
	@Operation(summary = "Archive a project.")
	@PreAuthorize("@securityService.hasPermission('PROYECTOS_WRITE')")
	public void archiveProject(@PathVariable UUID id) {
		UUID orgId = TenantContext.getRequiredTenantId();
		projectService.archiveProject(orgId, id);
	}

	@GetMapping("/{id}/team")
	@Operation(summary = "Get project team members.")
	@PreAuthorize("@securityService.hasPermission('PROYECTOS_READ')")
	public List<TeamMemberDto> getTeam(@PathVariable UUID id) {
		UUID orgId = TenantContext.getRequiredTenantId();
		return projectService.getTeamMembers(orgId, id).stream()
				.map(u -> new TeamMemberDto(u.id(), u.username(), u.email()))
				.toList();
	}

	@PostMapping("/{id}/team")
	@Operation(summary = "Add a team member to a project.")
	@PreAuthorize("@securityService.hasPermission('PROYECTOS_WRITE')")
	public void addTeamMember(@PathVariable UUID id, @Valid @RequestBody AddTeamMemberRequest request) {
		UUID orgId = TenantContext.getRequiredTenantId();
		projectService.addTeamMember(orgId, id, request.userId());
	}

	@DeleteMapping("/{id}/team/{userId}")
	@Operation(summary = "Remove a team member from a project.")
	@PreAuthorize("@securityService.hasPermission('PROYECTOS_WRITE')")
	public void removeTeamMember(@PathVariable UUID id, @PathVariable UUID userId) {
		UUID orgId = TenantContext.getRequiredTenantId();
		projectService.removeTeamMember(orgId, id, userId);
	}

	private ProjectDto toDto(Project p) {
		return new ProjectDto(
				p.id(),
				p.organizationId(),
				p.code(),
				p.name(),
				p.clientId(),
				p.description(),
				p.workType(),
				p.location(),
				p.estimatedStartDate(),
				p.estimatedEndDate(),
				p.currencyCode(),
				p.targetAmount(),
				p.minimumMargin(),
				p.responsibleUserId(),
				p.currentBudgetId(),
				p.status().name()
		);
	}

	private ProjectService.CreateProjectCmd toCmd(CreateProjectRequest r) {
		return new ProjectService.CreateProjectCmd(
				r.code(),
				r.name(),
				r.clientId(),
				r.description(),
				r.workType(),
				r.location(),
				r.estimatedStartDate(),
				r.estimatedEndDate(),
				r.currencyCode(),
				r.targetAmount(),
				r.minimumMargin(),
				r.responsibleUserId()
		);
	}

	public record CreateProjectRequest(
			@NotBlank @Size(min = 2, max = 60) String code,
			@NotBlank @Size(min = 3, max = 200) String name,
			UUID clientId,
			String description,
			String workType,
			String location,
			LocalDate estimatedStartDate,
			LocalDate estimatedEndDate,
			@NotBlank @Size(min = 3, max = 3) String currencyCode,
			BigDecimal targetAmount,
			BigDecimal minimumMargin,
			UUID responsibleUserId
	) {}

	public record AddTeamMemberRequest(
			@NotNull UUID userId
	) {}

	public record TeamMemberDto(UUID userId, String username, String email) {}

	public record ProjectDto(
			UUID id,
			UUID organizationId,
			String code,
			String name,
			UUID clientId,
			String description,
			String workType,
			String location,
			LocalDate estimatedStartDate,
			LocalDate estimatedEndDate,
			String currencyCode,
			BigDecimal targetAmount,
			BigDecimal minimumMargin,
			UUID responsibleUserId,
			UUID currentBudgetId,
			String status
	) {}
}
