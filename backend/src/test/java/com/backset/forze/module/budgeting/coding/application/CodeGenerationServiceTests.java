package com.backset.forze.module.budgeting.coding.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.backset.forze.module.budgeting.domain.budget.Budget;
import com.backset.forze.module.budgeting.domain.catalog.ApuMaestro;
import com.backset.forze.module.budgeting.domain.catalog.Insumo;
import com.backset.forze.module.budgeting.domain.catalog.InsumoType;
import com.backset.forze.module.budgeting.domain.catalog.RubroMaestro;
import com.backset.forze.module.budgeting.domain.project.Project;
import com.backset.forze.module.budgeting.infrastructure.ApuMaestroRepository;
import com.backset.forze.module.budgeting.infrastructure.BudgetRepository;
import com.backset.forze.module.budgeting.infrastructure.InsumoRepository;
import com.backset.forze.module.budgeting.infrastructure.ProjectRepository;
import com.backset.forze.module.budgeting.infrastructure.RubroMaestroRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CodeGenerationServiceTests {

	private ProjectRepository projects;
	private BudgetRepository budgets;
	private InsumoRepository insumos;
	private ApuMaestroRepository apus;
	private RubroMaestroRepository rubros;
	private CodeGenerationService service;

	private final UUID org = UUID.randomUUID();
	private final UUID projectId = UUID.randomUUID();
	private final int year = LocalDate.now().getYear();

	@BeforeEach
	void setUp() {
		projects = mock(ProjectRepository.class);
		budgets = mock(BudgetRepository.class);
		insumos = mock(InsumoRepository.class);
		apus = mock(ApuMaestroRepository.class);
		rubros = mock(RubroMaestroRepository.class);
		service = new CodeGenerationService(projects, budgets, insumos, apus, rubros);
	}

	private Project project(String code) {
		return new Project(UUID.randomUUID(), org, code, "P", "USD");
	}

	private Insumo insumo(String code) {
		return new Insumo(UUID.randomUUID(), org, code, "I", UUID.randomUUID(), InsumoType.MATERIAL);
	}

	private ApuMaestro apu(String code) {
		return new ApuMaestro(UUID.randomUUID(), org, code, "A", UUID.randomUUID(), 1);
	}

	private RubroMaestro rubro(String code) {
		return new RubroMaestro(UUID.randomUUID(), org, code, "R", UUID.randomUUID());
	}

	private Budget budget(String code) {
		return new Budget(UUID.randomUUID(), org, projectId, code, "B", "USD");
	}

	@Test
	void firstProjectCodeStartsAtOne() {
		when(projects.findByOrganizationId(org)).thenReturn(List.of());
		assertThat(service.nextProjectCode(org)).isEqualTo("PRY-" + year + "-0001");
	}

	@Test
	void nextProjectCodeIsMaxPlusOneIgnoringForeignCodes() {
		when(projects.findByOrganizationId(org)).thenReturn(List.of(
				project("PRY-" + year + "-0001"),
				project("PRY-" + year + "-0003"),
				project("DEMO-PRJ-009"),   // ignored: different pattern
				project("PRY-" + (year - 1) + "-0008"))); // ignored: different year prefix
		assertThat(service.nextProjectCode(org)).isEqualTo("PRY-" + year + "-0004");
	}

	@Test
	void nextBudgetCodeIsScopedToProject() {
		when(budgets.findByProjectId(projectId)).thenReturn(List.of(budget("PRE-" + year + "-0002")));
		assertThat(service.nextBudgetCode(org, projectId)).isEqualTo("PRE-" + year + "-0003");
	}

	@Test
	void nextInsumoCodeIgnoresNonMatching() {
		when(insumos.findByOrganizationId(org)).thenReturn(List.of(insumo("INS-0007"), insumo("DEMO-INS-CEM")));
		assertThat(service.nextInsumoCode(org)).isEqualTo("INS-0008");
	}

	@Test
	void nextApuCodeStartsAtOneWhenEmpty() {
		when(apus.findByOrganizationId(org)).thenReturn(List.of());
		assertThat(service.nextApuCode(org)).isEqualTo("APU-0001");
	}

	@Test
	void nextRubroCodeIsMaxPlusOne() {
		when(rubros.findByOrganizationId(org)).thenReturn(List.of(rubro("RUB-0010"), rubro("RUB-0004")));
		assertThat(service.nextRubroCode(org)).isEqualTo("RUB-0011");
	}
}
