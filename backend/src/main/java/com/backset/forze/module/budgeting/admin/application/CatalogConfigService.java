package com.backset.forze.module.budgeting.admin.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.backset.forze.module.budgeting.domain.admin.Category;
import com.backset.forze.module.budgeting.domain.admin.TaxConfig;
import com.backset.forze.module.budgeting.domain.admin.UnitOfMeasure;
import com.backset.forze.module.budgeting.infrastructure.CategoryRepository;
import com.backset.forze.module.budgeting.infrastructure.TaxConfigRepository;
import com.backset.forze.module.budgeting.infrastructure.UnitOfMeasureRepository;
import com.backset.forze.shared.api.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogConfigService {

	private final UnitOfMeasureRepository unitRepository;
	private final CategoryRepository categoryRepository;
	private final TaxConfigRepository taxRepository;

	public CatalogConfigService(
			UnitOfMeasureRepository unitRepository,
			CategoryRepository categoryRepository,
			TaxConfigRepository taxRepository
	) {
		this.unitRepository = unitRepository;
		this.categoryRepository = categoryRepository;
		this.taxRepository = taxRepository;
	}

	@Transactional(readOnly = true)
	public List<UnitOfMeasure> getUnits(UUID organizationId) {
		return unitRepository.findByOrganizationId(organizationId);
	}

	@Transactional
	public UnitOfMeasure createUnit(UUID organizationId, String code, String name) {
		unitRepository.findByOrganizationIdAndCode(organizationId, code).ifPresent(u -> {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Ya existe una unidad de medida con ese codigo.");
		});
		UnitOfMeasure unit = new UnitOfMeasure(UUID.randomUUID(), organizationId, code, name);
		return unitRepository.save(unit);
	}

	@Transactional(readOnly = true)
	public List<Category> getCategories(UUID organizationId) {
		return categoryRepository.findByOrganizationId(organizationId);
	}

	@Transactional
	public Category createCategory(UUID organizationId, String code, String name) {
		categoryRepository.findByOrganizationIdAndCode(organizationId, code).ifPresent(c -> {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Ya existe una categoria con ese codigo.");
		});
		Category category = new Category(UUID.randomUUID(), organizationId, code, name);
		return categoryRepository.save(category);
	}

	@Transactional(readOnly = true)
	public List<TaxConfig> getTaxes(UUID organizationId) {
		return taxRepository.findByOrganizationId(organizationId);
	}

	@Transactional
	public TaxConfig createTax(UUID organizationId, String code, String name, BigDecimal rate) {
		taxRepository.findByOrganizationIdAndCode(organizationId, code).ifPresent(t -> {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Ya existe una configuracion de impuesto con ese codigo.");
		});
		TaxConfig tax = new TaxConfig(UUID.randomUUID(), organizationId, code, name, rate);
		return taxRepository.save(tax);
	}
}
