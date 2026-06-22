package com.backset.forze.module.budgeting.demo;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.backset.forze.configuration.DemoProperties;
import com.backset.forze.module.budgeting.admin.application.CatalogConfigService;
import com.backset.forze.module.budgeting.admin.application.MembershipService;
import com.backset.forze.module.budgeting.admin.application.OrganizationService;
import com.backset.forze.module.budgeting.approval.application.ApprovalService;
import com.backset.forze.module.budgeting.budget.application.BudgetService;
import com.backset.forze.module.budgeting.budget.application.BudgetVersionCalculationService;
import com.backset.forze.module.budgeting.catalog.application.CatalogService;
import com.backset.forze.module.budgeting.document.application.BudgetDocumentService;
import com.backset.forze.module.budgeting.project.application.ClientService;
import com.backset.forze.module.budgeting.project.application.ProjectService;
import com.backset.forze.module.budgeting.scenario.application.ScenarioService;
import com.backset.forze.module.budgeting.supplier.application.SupplierService;
import com.backset.forze.module.identity.infrastructure.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * The demo seeder must be safe to run repeatedly (idempotent) and must never
 * touch a production environment.
 */
class DemoDataSeederTests {

	private Environment environment;
	private UserAccountRepository users;
	private OrganizationService organizationService;
	private DemoDataSeeder seeder;

	@BeforeEach
	void setUp() {
		environment = mock(Environment.class);
		users = mock(UserAccountRepository.class);
		organizationService = mock(OrganizationService.class);
		seeder = new DemoDataSeeder(
				mock(DemoProperties.class),
				environment,
				users,
				mock(PasswordEncoder.class),
				organizationService,
				mock(MembershipService.class),
				mock(CatalogConfigService.class),
				mock(ClientService.class),
				mock(ProjectService.class),
				mock(CatalogService.class),
				mock(SupplierService.class),
				mock(BudgetService.class),
				mock(BudgetVersionCalculationService.class),
				mock(ScenarioService.class),
				mock(ApprovalService.class),
				mock(BudgetDocumentService.class));
	}

	@Test
	void doesNotSeedWhenSentinelAccountAlreadyExists() {
		when(environment.getActiveProfiles()).thenReturn(new String[] { "dev" });
		when(users.existsByUsername("admin.demo@forze.local")).thenReturn(true);

		seeder.run(null);

		// Idempotent: a second run creates nothing.
		verifyNoInteractions(organizationService);
	}

	@Test
	void seedsWhenSentinelAccountIsMissing() {
		when(environment.getActiveProfiles()).thenReturn(new String[] { "dev" });
		when(users.existsByUsername("admin.demo@forze.local")).thenReturn(false);
		// Abort the cascade early but prove seeding was attempted.
		when(organizationService.createOrganization(any(), any()))
				.thenThrow(new RuntimeException("stop after first creation"));

		try {
			seeder.run(null);
		}
		catch (RuntimeException ignored) {
			// expected: cascade halted right after the org is created
		}

		verify(organizationService).createOrganization(any(), any());
	}

	@Test
	void doesNotSeedUnderProdProfile() {
		when(environment.getActiveProfiles()).thenReturn(new String[] { "prod" });

		seeder.run(null);

		// Hard production block: never even checks for demo data.
		verifyNoInteractions(users);
		verifyNoInteractions(organizationService);
	}
}
