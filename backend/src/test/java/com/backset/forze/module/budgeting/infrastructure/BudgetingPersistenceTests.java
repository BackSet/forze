package com.backset.forze.module.budgeting.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;

import com.backset.forze.module.budgeting.domain.admin.Organization;
import com.backset.forze.module.budgeting.domain.admin.UnitOfMeasure;
import com.backset.forze.module.budgeting.domain.budget.Budget;
import com.backset.forze.module.budgeting.domain.budget.BudgetItem;
import com.backset.forze.module.budgeting.domain.budget.BudgetVersion;
import com.backset.forze.module.budgeting.domain.budget.Chapter;
import com.backset.forze.module.budgeting.domain.budget.ItemApu;
import com.backset.forze.module.budgeting.domain.budget.ItemApuComponent;
import com.backset.forze.module.budgeting.domain.catalog.ComponentSection;
import com.backset.forze.module.budgeting.domain.catalog.Insumo;
import com.backset.forze.module.budgeting.domain.catalog.InsumoType;
import com.backset.forze.module.budgeting.domain.project.Project;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Persistence tests for the budgeting module against a real PostgreSQL.
 * Booting the context runs all migrations from scratch and Hibernate {@code ddl-auto=validate},
 * so a green context already proves migrations and JPA mappings agree.
 * Skipped automatically when Docker is unavailable.
 */
@SpringBootTest(properties = "debug=false")
@Testcontainers(disabledWithoutDocker = true)
class BudgetingPersistenceTests {

	@Container
	static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine");

	@DynamicPropertySource
	static void datasourceProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
	}

	@Autowired
	private OrganizationRepository organizations;

	@Autowired
	private UnitOfMeasureRepository units;

	@Autowired
	private InsumoRepository insumos;

	@Autowired
	private ProjectRepository projects;

	@Autowired
	private BudgetRepository budgets;

	@Autowired
	private BudgetVersionRepository budgetVersions;

	@Autowired
	private ChapterRepository chapters;

	@Autowired
	private BudgetItemRepository budgetItems;

	@Autowired
	private ItemApuRepository itemApus;

	@Autowired
	private ItemApuComponentRepository itemApuComponents;

	private Organization newOrganization() {
		return organizations.save(new Organization(UUID.randomUUID(), "Constructora Demo"));
	}

	private UnitOfMeasure newUnit(UUID organizationId, String code) {
		return units.save(new UnitOfMeasure(UUID.randomUUID(), organizationId, code, code));
	}

	@Test
	void contextLoadsAndValidatesSchema() {
		assertThat(organizations.count()).isGreaterThanOrEqualTo(0);
	}

	@Test
	void persistsNumericPrecisionForMoneyAndQuantity() {
		Organization org = newOrganization();
		UnitOfMeasure unit = newUnit(org.id(), "M3");
		Insumo insumo = new Insumo(UUID.randomUUID(), org.id(), "CEM-001", "Cemento", unit.id(), InsumoType.MATERIAL);
		insumo.updateReferencePrice(new BigDecimal("9.8000"), "USD");
		insumos.save(insumo);

		Insumo reloaded = insumos.findById(insumo.id()).orElseThrow();
		assertThat(reloaded.referencePrice()).isEqualByComparingTo(new BigDecimal("9.8000"));
		assertThat(reloaded.referencePrice().scale()).isEqualTo(4);
		assertThat(reloaded.referencePriceCurrency()).isEqualTo("USD");
	}

	@Test
	void enforcesUniqueCodePerOrganization() {
		Organization org = newOrganization();
		UnitOfMeasure unit = newUnit(org.id(), "KG");
		insumos.save(new Insumo(UUID.randomUUID(), org.id(), "DUP-1", "A", unit.id(), InsumoType.MATERIAL));

		Insumo duplicate = new Insumo(UUID.randomUUID(), org.id(), "DUP-1", "B", unit.id(), InsumoType.MATERIAL);
		assertThatThrownBy(() -> insumos.saveAndFlush(duplicate))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void restrictsDeletionOfReferencedUnit() {
		Organization org = newOrganization();
		UnitOfMeasure unit = newUnit(org.id(), "U");
		insumos.save(new Insumo(UUID.randomUUID(), org.id(), "REF-1", "A", unit.id(), InsumoType.MATERIAL));

		assertThatThrownBy(() -> units.deleteById(unit.id()))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void componentSnapshotDoesNotChangeWhenCatalogPriceChanges() {
		Organization org = newOrganization();
		UnitOfMeasure unit = newUnit(org.id(), "M2");
		Insumo insumo = new Insumo(UUID.randomUUID(), org.id(), "PNT-1", "Pintura", unit.id(), InsumoType.MATERIAL);
		insumo.updateReferencePrice(new BigDecimal("10.0000"), "USD");
		insumos.save(insumo);

		ItemApuComponent component = buildBudgetTreeWithComponent(org.id(), unit.id(), insumo.id(),
				new BigDecimal("9.8000"));

		// Catalog price changes after the snapshot was taken.
		Insumo catalog = insumos.findById(insumo.id()).orElseThrow();
		catalog.updateReferencePrice(new BigDecimal("12.5000"), "USD");
		insumos.save(catalog);

		ItemApuComponent frozen = itemApuComponents.findById(component.id()).orElseThrow();
		assertThat(frozen.unitPrice()).isEqualByComparingTo(new BigDecimal("9.8000"));
		assertThat(frozen.sourceInsumoId()).isEqualTo(insumo.id());
	}

	@Test
	void cascadeDeletesBudgetVersionTree() {
		Organization org = newOrganization();
		UnitOfMeasure unit = newUnit(org.id(), "GL");
		Insumo insumo = insumos.save(
				new Insumo(UUID.randomUUID(), org.id(), "CAS-1", "Insumo", unit.id(), InsumoType.MATERIAL));
		ItemApuComponent component = buildBudgetTreeWithComponent(org.id(), unit.id(), insumo.id(),
				new BigDecimal("5.0000"));
		UUID itemApuId = component.itemApuId();
		ItemApu itemApu = itemApus.findById(itemApuId).orElseThrow();
		BudgetItem item = budgetItems.findById(itemApu.budgetItemId()).orElseThrow();
		BudgetVersion version = budgetVersions.findById(item.budgetVersionId()).orElseThrow();

		budgets.deleteById(version.budgetId());

		assertThat(budgetVersions.findById(version.id())).isEmpty();
		assertThat(budgetItems.findById(item.id())).isEmpty();
		assertThat(itemApus.findById(itemApuId)).isEmpty();
		assertThat(itemApuComponents.findById(component.id())).isEmpty();
	}

	@Test
	void detectsOptimisticLockConflictOnBudgetVersion() {
		Organization org = newOrganization();
		Project project = projects.save(new Project(UUID.randomUUID(), org.id(), "PRJ-OPT", "Proyecto", "USD"));
		Budget budget = budgets.save(
				new Budget(UUID.randomUUID(), org.id(), project.id(), "BUD-1", "Presupuesto", "USD"));
		BudgetVersion saved = budgetVersions.save(new BudgetVersion(UUID.randomUUID(), budget.id(), 1));

		BudgetVersion first = budgetVersions.findById(saved.id()).orElseThrow();
		BudgetVersion stale = budgetVersions.findById(saved.id()).orElseThrow();

		first.describe("V1", "ok", null, null);
		budgetVersions.saveAndFlush(first);

		stale.describe("V1-stale", "conflict", null, null);
		assertThatThrownBy(() -> budgetVersions.saveAndFlush(stale))
				.isInstanceOf(OptimisticLockingFailureException.class);
	}

	private ItemApuComponent buildBudgetTreeWithComponent(UUID organizationId, UUID unitId, UUID insumoId,
			BigDecimal componentUnitPrice) {
		Project project = projects.save(
				new Project(UUID.randomUUID(), organizationId, "P-" + shortId(), "Proyecto", "USD"));
		Budget budget = budgets.save(
				new Budget(UUID.randomUUID(), organizationId, project.id(), "B-" + shortId(), "Presupuesto", "USD"));
		BudgetVersion version = budgetVersions.save(new BudgetVersion(UUID.randomUUID(), budget.id(), 1));
		Chapter chapter = chapters.save(new Chapter(UUID.randomUUID(), version.id(), "Capitulo 1", 1));
		BudgetItem item = new BudgetItem(UUID.randomUUID(), version.id(), "Rubro", unitId, new BigDecimal("1.0000"), 1);
		item.placeInChapter(chapter.id());
		budgetItems.save(item);
		ItemApu itemApu = itemApus.save(new ItemApu(UUID.randomUUID(), item.id(), null, new BigDecimal("1.000000")));
		ItemApuComponent component = new ItemApuComponent(UUID.randomUUID(), itemApu.id(), ComponentSection.MATERIALES,
				unitId, new BigDecimal("1.0000"), componentUnitPrice, 1);
		component.describe(insumoId, "Componente", null, null, "PRECIO_HISTORICO");
		return itemApuComponents.save(component);
	}

	private static String shortId() {
		return UUID.randomUUID().toString().substring(0, 8);
	}
}
