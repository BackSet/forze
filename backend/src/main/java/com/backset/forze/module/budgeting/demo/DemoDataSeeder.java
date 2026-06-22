package com.backset.forze.module.budgeting.demo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.backset.forze.configuration.DemoProperties;
import com.backset.forze.module.budgeting.admin.application.CatalogConfigService;
import com.backset.forze.module.budgeting.admin.application.MembershipService;
import com.backset.forze.module.budgeting.admin.application.OrganizationService;
import com.backset.forze.module.budgeting.budget.application.BudgetService;
import com.backset.forze.module.budgeting.budget.application.BudgetVersionCalculationService;
import com.backset.forze.module.budgeting.catalog.application.CatalogService;
import com.backset.forze.module.budgeting.document.application.BudgetDocumentService;
import com.backset.forze.module.budgeting.approval.application.ApprovalService;
import com.backset.forze.module.budgeting.domain.admin.Organization;
import com.backset.forze.module.budgeting.domain.approval.ApprovalRequest;
import com.backset.forze.module.budgeting.domain.budget.Budget;
import com.backset.forze.module.budgeting.domain.budget.BudgetItem;
import com.backset.forze.module.budgeting.domain.budget.BudgetVersion;
import com.backset.forze.module.budgeting.domain.budget.Chapter;
import com.backset.forze.module.budgeting.domain.admin.Category;
import com.backset.forze.module.budgeting.domain.admin.UnitOfMeasure;
import com.backset.forze.module.budgeting.domain.catalog.ComponentSection;
import com.backset.forze.module.budgeting.domain.catalog.Insumo;
import com.backset.forze.module.budgeting.domain.catalog.InsumoType;
import com.backset.forze.module.budgeting.domain.catalog.RubroMaestro;
import com.backset.forze.module.budgeting.domain.document.DocumentType;
import com.backset.forze.module.budgeting.domain.scenario.ScenarioType;
import com.backset.forze.module.budgeting.project.application.ClientService;
import com.backset.forze.module.budgeting.project.application.ProjectService;
import com.backset.forze.module.budgeting.scenario.application.ScenarioService;
import com.backset.forze.module.budgeting.supplier.application.SupplierService;
import com.backset.forze.module.identity.domain.UserAccount;
import com.backset.forze.module.identity.infrastructure.UserAccountRepository;
import com.backset.forze.shared.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads realistic demo data into a local/dev database so the whole system can be
 * explored. It only ever runs under the {@code dev} profile (and is hard-blocked
 * if {@code prod} is active), is idempotent (a sentinel account short-circuits a
 * second run), and reuses the domain application services so totals/viability are
 * computed by the real calculation pipeline — never invented.
 *
 * <p>All accounts and entities are fictitious and namespaced with a {@code DEMO-}
 * prefix; no real personal data is used and passwords are never logged.
 */
@Component
@Profile("dev")
@ConditionalOnProperty(prefix = "forze.demo", name = "enabled", havingValue = "true", matchIfMissing = true)
class DemoDataSeeder implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

	/** Fictitious, local-only shared password for every demo account. Never logged. */
	private static final String DEFAULT_DEMO_PASSWORD = "Demo1234!";

	private static final String ADMIN_USER = "admin.demo@forze.local";
	private static final String PRESUPUESTISTA_USER = "presupuestista.demo@forze.local";
	private static final String APROBADOR_USER = "aprobador.demo@forze.local";
	private static final String COMPRAS_USER = "compras.demo@forze.local";

	private final DemoProperties demoProperties;
	private final Environment environment;
	private final UserAccountRepository users;
	private final PasswordEncoder passwordEncoder;
	private final OrganizationService organizationService;
	private final MembershipService membershipService;
	private final CatalogConfigService catalogConfigService;
	private final ClientService clientService;
	private final ProjectService projectService;
	private final CatalogService catalogService;
	private final SupplierService supplierService;
	private final BudgetService budgetService;
	private final BudgetVersionCalculationService versionCalculationService;
	private final ScenarioService scenarioService;
	private final ApprovalService approvalService;
	private final BudgetDocumentService documentService;

	DemoDataSeeder(DemoProperties demoProperties, Environment environment, UserAccountRepository users,
			PasswordEncoder passwordEncoder, OrganizationService organizationService, MembershipService membershipService,
			CatalogConfigService catalogConfigService, ClientService clientService, ProjectService projectService,
			CatalogService catalogService, SupplierService supplierService, BudgetService budgetService,
			BudgetVersionCalculationService versionCalculationService, ScenarioService scenarioService,
			ApprovalService approvalService, BudgetDocumentService documentService) {
		this.demoProperties = demoProperties;
		this.environment = environment;
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.organizationService = organizationService;
		this.membershipService = membershipService;
		this.catalogConfigService = catalogConfigService;
		this.clientService = clientService;
		this.projectService = projectService;
		this.catalogService = catalogService;
		this.supplierService = supplierService;
		this.budgetService = budgetService;
		this.versionCalculationService = versionCalculationService;
		this.scenarioService = scenarioService;
		this.approvalService = approvalService;
		this.documentService = documentService;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		// Explicit production block (in addition to @Profile("dev")).
		for (String profile : environment.getActiveProfiles()) {
			if ("prod".equalsIgnoreCase(profile)) {
				log.warn("Demo data seeding is disabled under the 'prod' profile; skipping.");
				return;
			}
		}

		// Idempotency: if the sentinel demo account exists the seed already ran.
		if (users.existsByUsername(ADMIN_USER)) {
			log.info("Demo data already present; skipping seed (idempotent).");
			return;
		}

		log.info("Seeding DEMO data with fictitious local-only accounts (passwords are not logged).");
		seed();
		log.info("DEMO data seeded.");
	}

	private void seed() {
		String rawPassword = demoProperties.password() != null && !demoProperties.password().isBlank()
				? demoProperties.password()
				: DEFAULT_DEMO_PASSWORD;
		String hash = passwordEncoder.encode(rawPassword);

		// --- Users (no UserService exists; create via repository + encoder) ---
		UUID adminId = createUser(ADMIN_USER, hash);
		UUID presupuestistaId = createUser(PRESUPUESTISTA_USER, hash);
		UUID aprobadorId = createUser(APROBADOR_USER, hash);
		UUID comprasId = createUser(COMPRAS_USER, hash);

		// --- Organization (creator becomes ADMINISTRADOR automatically) + members ---
		Organization org = organizationService.createOrganization("DEMO - Constructora Andina", adminId);
		UUID orgId = org.id();
		// Service-layer reads use orgId params; set the tenant defensively for any
		// component that consults TenantContext during the seed.
		TenantContext.setTenantId(orgId);
		try {
			membershipService.addMember(orgId, adminId, PRESUPUESTISTA_USER, "PRESUPUESTISTA");
			membershipService.addMember(orgId, adminId, APROBADOR_USER, "APROBADOR");
			membershipService.addMember(orgId, adminId, COMPRAS_USER, "COMPRAS");

			seedCatalogAndBudgets(orgId, adminId, presupuestistaId, aprobadorId, comprasId);
		}
		finally {
			TenantContext.clear();
		}
	}

	private void seedCatalogAndBudgets(UUID orgId, UUID adminId, UUID presupuestistaId, UUID aprobadorId, UUID comprasId) {
		// --- Units / categories / tax ---
		UUID uM3 = unit(orgId, "DEMO-M3", "Metro cubico");
		UUID uM2 = unit(orgId, "DEMO-M2", "Metro cuadrado");
		UUID uKg = unit(orgId, "DEMO-KG", "Kilogramo");
		UUID uHora = unit(orgId, "DEMO-HORA", "Hora");
		Category catMat = catalogConfigService.createCategory(orgId, "DEMO-MAT", "Materiales");
		Category catMo = catalogConfigService.createCategory(orgId, "DEMO-MO", "Mano de obra");
		Category catEq = catalogConfigService.createCategory(orgId, "DEMO-EQ", "Equipos");
		UUID taxIva = catalogConfigService.createTax(orgId, "DEMO-IVA", "IVA 15%", new BigDecimal("0.15")).id();

		// --- Insumos ---
		UUID insCemento = insumo(orgId, "DEMO-INS-CEM", "Cemento Portland", uKg, InsumoType.MATERIAL, catMat.id(), "0.18");
		UUID insArena = insumo(orgId, "DEMO-INS-ARE", "Arena gruesa", uM3, InsumoType.MATERIAL, catMat.id(), "12.00");
		UUID insRipio = insumo(orgId, "DEMO-INS-RIP", "Ripio triturado", uM3, InsumoType.MATERIAL, catMat.id(), "14.00");
		UUID insAcero = insumo(orgId, "DEMO-INS-ACE", "Acero de refuerzo", uKg, InsumoType.MATERIAL, catMat.id(), "1.10");
		UUID insAlbanil = insumo(orgId, "DEMO-INS-ALB", "Albanil", uHora, InsumoType.MANO_DE_OBRA, catMo.id(), "4.50");
		UUID insPeon = insumo(orgId, "DEMO-INS-PEO", "Peon", uHora, InsumoType.MANO_DE_OBRA, catMo.id(), "3.80");
		UUID insConcretera = insumo(orgId, "DEMO-INS-CON", "Concretera", uHora, InsumoType.EQUIPO, catEq.id(), "6.00");

		// --- Master APUs (real cost from components) ---
		UUID apuHormigon = catalogService.createApu(orgId,
				new CatalogService.CreateApuCmd("DEMO-APU-HOR", "Hormigon f'c=210 kg/cm2", uM3, BigDecimal.ONE, LocalDate.now().plusYears(1))).id();
		addComp(orgId, apuHormigon, ComponentSection.MATERIALES, insCemento, uKg, "350", "1", "0.05", "0.18");
		addComp(orgId, apuHormigon, ComponentSection.MATERIALES, insArena, uM3, "0.65", "1", "0.05", "12.00");
		addComp(orgId, apuHormigon, ComponentSection.MATERIALES, insRipio, uM3, "0.65", "1", "0.05", "14.00");
		addComp(orgId, apuHormigon, ComponentSection.MANO_DE_OBRA, insAlbanil, uHora, "8", "1", "0", "4.50");
		addComp(orgId, apuHormigon, ComponentSection.MANO_DE_OBRA, insPeon, uHora, "8", "1", "0", "3.80");
		addComp(orgId, apuHormigon, ComponentSection.EQUIPOS, insConcretera, uHora, "1", "1", "0", "6.00");

		UUID apuAcero = catalogService.createApu(orgId,
				new CatalogService.CreateApuCmd("DEMO-APU-ACE", "Acero de refuerzo fy=4200", uKg, BigDecimal.ONE, LocalDate.now().plusYears(1))).id();
		addComp(orgId, apuAcero, ComponentSection.MATERIALES, insAcero, uKg, "1.05", "1", "0.03", "1.10");
		addComp(orgId, apuAcero, ComponentSection.MANO_DE_OBRA, insAlbanil, uHora, "0.03", "1", "0", "4.50");
		addComp(orgId, apuAcero, ComponentSection.MANO_DE_OBRA, insPeon, uHora, "0.03", "1", "0", "3.80");

		// --- Rubros (with base APU; one without APU to exercise alerts) ---
		UUID rubHormigon = rubro(orgId, "DEMO-RUB-HOR", "Hormigon estructural f'c=210", uM3, catMat.id(), apuHormigon);
		UUID rubAcero = rubro(orgId, "DEMO-RUB-ACE", "Acero de refuerzo fy=4200", uKg, catMat.id(), apuAcero);
		UUID rubExcavacion = rubro(orgId, "DEMO-RUB-EXC", "Excavacion manual", uM3, catMo.id(), null);

		// --- Supplier + quotation (auto-creates price history -> price comparator) ---
		UUID supplierId = supplierService.createSupplier(orgId, new SupplierService.CreateSupplierCmd(
				"DEMO - Proveedora Andina S.A.", "DEMO-RUC-001", "Contacto Demo", "099-000-0000",
				"ventas.demo@forze.local", "Quito", "Materiales de construccion", "30 dias", "48h", new BigDecimal("4.5"))).id();
		supplierService.createQuotation(orgId, new SupplierService.CreateQuotationCmd(
				supplierId, LocalDate.now(), LocalDate.now().plusMonths(1), "USD", taxIva, new BigDecimal("25.00"),
				"Precios sujetos a stock", null, "Quito",
				List.of(
						new SupplierService.QuotationItemCmd(insCemento, "Cemento Portland", uKg, new BigDecimal("0.17"), new BigDecimal("100"), BigDecimal.ZERO, true, false),
						new SupplierService.QuotationItemCmd(insArena, "Arena gruesa", uM3, new BigDecimal("11.50"), BigDecimal.ONE, BigDecimal.ZERO, true, false),
						new SupplierService.QuotationItemCmd(insAcero, "Acero de refuerzo", uKg, new BigDecimal("1.05"), new BigDecimal("50"), BigDecimal.ZERO, true, false))));

		// --- Clients + projects ---
		UUID clienteNorte = clientService.createClient(orgId, "DEMO - Cliente Norte S.A.").id();
		UUID clienteSur = clientService.createClient(orgId, "DEMO - Cliente Sur Ltda.").id();

		UUID proyectoA = projectService.createProject(orgId, new ProjectService.CreateProjectCmd(
				"DEMO-PRJ-001", "Edificio Residencial Demo", clienteNorte, "Edificio de 5 plantas", "EDIFICACION",
				"Quito", LocalDate.now(), LocalDate.now().plusMonths(8), "USD", new BigDecimal("500000.00"), new BigDecimal("0.10"), presupuestistaId)).id();
		UUID proyectoB = projectService.createProject(orgId, new ProjectService.CreateProjectCmd(
				"DEMO-PRJ-002", "Bodega Industrial Demo", clienteSur, "Galpon industrial", "INDUSTRIAL",
				"Guayaquil", LocalDate.now(), LocalDate.now().plusMonths(5), "USD", new BigDecimal("250000.00"), new BigDecimal("0.10"), presupuestistaId)).id();

		// --- Budget A: complete, viable, approved (immutable) ---
		BudgetVersion versionA = seedViableBudget(orgId, proyectoA, presupuestistaId, taxIva, rubHormigon, rubAcero);
		ApprovalRequest request = approvalService.submitForApproval(orgId, versionA.id(), presupuestistaId);
		approvalService.approve(orgId, request.id(), aprobadorId);
		// Document metadata for the approved (client-facing) version.
		documentService.generatePdf(orgId, versionA.id(), DocumentType.COTIZACION, presupuestistaId);

		// --- Budget B: calculated but with quality alerts; kept editable ---
		BudgetVersion versionB = seedBudgetWithAlerts(orgId, proyectoB, presupuestistaId, taxIva, rubHormigon, rubExcavacion);

		// --- Scenario on the editable version (does not mutate the base) ---
		scenarioService.createScenario(orgId, versionB.id(), new ScenarioService.CreateScenarioCmd(
				"Escenario economico", ScenarioType.ECONOMICO, new BigDecimal("0.08"), new BigDecimal("0.06"),
				new BigDecimal("0.02"), 120, "Tradicional", List.of()));
	}

	private BudgetVersion seedViableBudget(UUID orgId, UUID projectId, UUID userId, UUID taxId, UUID rubHormigon, UUID rubAcero) {
		Budget budget = budgetService.createBudget(orgId, projectId, new BudgetService.CreateBudgetCmd(
				"DEMO-PPTO-001", "Presupuesto Base", "USD", userId));
		BudgetVersion v1 = firstVersion(budget.id());
		budgetService.configureFinancials(orgId, v1.id(), new BudgetService.ConfigureFinancialsCmd(
				new BigDecimal("500000.00"), new BigDecimal("0.10"), new BigDecimal("0.08"), new BigDecimal("0.03"), taxId, LocalDate.now().plusMonths(2)));

		Chapter cimentacion = budgetService.createChapter(v1.id(), "Cimentacion", null);
		Chapter estructura = budgetService.createChapter(v1.id(), "Estructura", null);

		BudgetItem hormItem = budgetService.addRubroToVersion(orgId, v1.id(), rubHormigon, cimentacion.id(), new BigDecimal("80"));
		budgetService.addRubroToVersion(orgId, v1.id(), rubAcero, estructura.id(), new BigDecimal("4500"));
		// Structured measurement feeds the quantity (10 x 5 x 1 = 50 m3).
		budgetService.addMeasurement(orgId, hormItem.id(), new BudgetService.AddMeasurementCmd(
				"Losa de cimentacion", new BigDecimal("10"), new BigDecimal("5"), new BigDecimal("1"), BigDecimal.ONE, BigDecimal.ONE, "10*5*1", null));

		return versionCalculationService.calculateVersion(orgId, v1.id());
	}

	private BudgetVersion seedBudgetWithAlerts(UUID orgId, UUID projectId, UUID userId, UUID taxId, UUID rubHormigon, UUID rubExcavacion) {
		Budget budget = budgetService.createBudget(orgId, projectId, new BudgetService.CreateBudgetCmd(
				"DEMO-PPTO-002", "Presupuesto con observaciones", "USD", userId));
		BudgetVersion v1 = firstVersion(budget.id());
		budgetService.configureFinancials(orgId, v1.id(), new BudgetService.ConfigureFinancialsCmd(
				new BigDecimal("250000.00"), new BigDecimal("0.10"), new BigDecimal("0.08"), new BigDecimal("0.03"), taxId, LocalDate.now().plusMonths(2)));

		Chapter cap = budgetService.createChapter(v1.id(), "Obra gris", null);
		budgetService.addRubroToVersion(orgId, v1.id(), rubHormigon, cap.id(), new BigDecimal("30"));
		// Quality-alert triggers: a zero-quantity item and a rubro without APU.
		budgetService.addRubroToVersion(orgId, v1.id(), rubHormigon, cap.id(), BigDecimal.ZERO);
		budgetService.addRubroToVersion(orgId, v1.id(), rubExcavacion, cap.id(), new BigDecimal("120"));

		return versionCalculationService.calculateVersion(orgId, v1.id());
	}

	// --- small helpers over the domain services ---

	private UUID createUser(String username, String passwordHash) {
		UUID id = UUID.randomUUID();
		users.save(new UserAccount(id, username, username, passwordHash, true));
		return id;
	}

	private UUID unit(UUID orgId, String code, String name) {
		UnitOfMeasure unit = catalogConfigService.createUnit(orgId, code, name);
		return unit.id();
	}

	private UUID insumo(UUID orgId, String code, String name, UUID unitId, InsumoType type, UUID categoryId, String referencePrice) {
		Insumo insumo = catalogService.createInsumo(orgId, new CatalogService.CreateInsumoCmd(
				code, name, name, unitId, type, categoryId, "DEMO", "", new BigDecimal(referencePrice), "USD"));
		return insumo.id();
	}

	private void addComp(UUID orgId, UUID apuId, ComponentSection section, UUID insumoId, UUID unitId,
			String quantity, String yield, String waste, String unitPrice) {
		catalogService.addComponent(orgId, apuId, new CatalogService.AddComponentCmd(
				section, insumoId, "", unitId, new BigDecimal(quantity), new BigDecimal(yield), new BigDecimal(waste), new BigDecimal(unitPrice)));
	}

	private UUID rubro(UUID orgId, String code, String name, UUID unitId, UUID categoryId, UUID baseApuId) {
		RubroMaestro rubro = catalogService.createRubro(orgId, new CatalogService.CreateRubroCmd(
				code, name, name, unitId, categoryId, "", "demo", baseApuId));
		return rubro.id();
	}

	private BudgetVersion firstVersion(UUID budgetId) {
		List<BudgetVersion> versions = budgetService.getVersionsForBudget(budgetId);
		return versions.get(0);
	}
}
