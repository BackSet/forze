package com.backset.forze.module.budgeting.project.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.backset.forze.module.budgeting.domain.project.Project;
import com.backset.forze.module.budgeting.domain.project.ProjectTeamMember;
import com.backset.forze.module.budgeting.infrastructure.ProjectRepository;
import com.backset.forze.module.budgeting.infrastructure.ProjectTeamMemberRepository;
import com.backset.forze.module.identity.domain.UserAccount;
import com.backset.forze.module.identity.infrastructure.UserAccountRepository;
import com.backset.forze.shared.api.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectService {

	private final ProjectRepository projectRepository;
	private final ProjectTeamMemberRepository teamMemberRepository;
	private final UserAccountRepository userAccountRepository;

	public ProjectService(
			ProjectRepository projectRepository,
			ProjectTeamMemberRepository teamMemberRepository,
			UserAccountRepository userAccountRepository
	) {
		this.projectRepository = projectRepository;
		this.teamMemberRepository = teamMemberRepository;
		this.userAccountRepository = userAccountRepository;
	}

	@Transactional(readOnly = true)
	public List<Project> getProjects(UUID organizationId) {
		return projectRepository.findByOrganizationId(organizationId);
	}

	@Transactional(readOnly = true)
	public Project getProject(UUID organizationId, UUID projectId) {
		Project project = projectRepository.findById(projectId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Proyecto no encontrado."));

		if (!project.organizationId().equals(organizationId)) {
			throw new ApiException(HttpStatus.FORBIDDEN, "El proyecto no pertenece a la organizacion activa.");
		}

		return project;
	}

	@Transactional
	public Project createProject(UUID organizationId, CreateProjectCmd cmd) {
		if (projectRepository.findByOrganizationIdAndCode(organizationId, cmd.code()).isPresent()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Ya existe un proyecto con ese codigo.");
		}

		Project project = new Project(UUID.randomUUID(), organizationId, cmd.code(), cmd.name(), cmd.currencyCode());
		project.describe(cmd.clientId(), cmd.description(), cmd.workType(), cmd.location(), cmd.estimatedStartDate(), cmd.estimatedEndDate());
		project.setFinancialTarget(cmd.targetAmount(), cmd.minimumMargin());
		project.assignResponsible(cmd.responsibleUserId());

		return projectRepository.save(project);
	}

	@Transactional
	public Project updateProject(UUID organizationId, UUID projectId, CreateProjectCmd cmd) {
		Project project = getProject(organizationId, projectId);

		projectRepository.findByOrganizationIdAndCode(organizationId, cmd.code())
				.filter(p -> !p.id().equals(projectId))
				.ifPresent(p -> {
					throw new ApiException(HttpStatus.BAD_REQUEST, "Ya existe otro proyecto con ese codigo.");
				});

		project.describe(cmd.clientId(), cmd.description(), cmd.workType(), cmd.location(), cmd.estimatedStartDate(), cmd.estimatedEndDate());
		project.setFinancialTarget(cmd.targetAmount(), cmd.minimumMargin());
		project.assignResponsible(cmd.responsibleUserId());

		return projectRepository.save(project);
	}

	@Transactional
	public void archiveProject(UUID organizationId, UUID projectId) {
		Project project = getProject(organizationId, projectId);
		project.archive();
		projectRepository.save(project);
	}

	@Transactional(readOnly = true)
	public List<UserAccount> getTeamMembers(UUID organizationId, UUID projectId) {
		getProject(organizationId, projectId);

		List<ProjectTeamMember> team = teamMemberRepository.findByIdProjectId(projectId);
		List<UUID> userIds = team.stream().map(ProjectTeamMember::userId).toList();
		return userAccountRepository.findAllById(userIds);
	}

	@Transactional
	public void addTeamMember(UUID organizationId, UUID projectId, UUID userId) {
		getProject(organizationId, projectId);

		if (!userAccountRepository.existsById(userId)) {
			throw new ApiException(HttpStatus.NOT_FOUND, "Usuario no encontrado.");
		}

		if (teamMemberRepository.existsById(new ProjectTeamMember.ProjectTeamMemberId(projectId, userId))) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "El usuario ya es miembro del equipo de este proyecto.");
		}

		ProjectTeamMember member = new ProjectTeamMember(projectId, userId);
		teamMemberRepository.save(member);
	}

	@Transactional
	public void removeTeamMember(UUID organizationId, UUID projectId, UUID userId) {
		getProject(organizationId, projectId);

		ProjectTeamMember.ProjectTeamMemberId id = new ProjectTeamMember.ProjectTeamMemberId(projectId, userId);
		ProjectTeamMember member = teamMemberRepository.findById(id)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Miembro del equipo no encontrado."));

		teamMemberRepository.delete(member);
	}

	public record CreateProjectCmd(
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
			UUID responsibleUserId
	) {}
}
