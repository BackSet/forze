package com.backset.forze.module.budgeting.catalog.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.backset.forze.module.budgeting.domain.catalog.ApuComponent;
import com.backset.forze.module.budgeting.domain.catalog.ApuMaestro;
import com.backset.forze.module.budgeting.domain.catalog.ApuStatus;
import com.backset.forze.module.budgeting.domain.catalog.CatalogStatus;
import com.backset.forze.module.budgeting.domain.catalog.ComponentSection;
import com.backset.forze.module.budgeting.domain.catalog.Insumo;
import com.backset.forze.module.budgeting.domain.catalog.InsumoType;
import com.backset.forze.module.budgeting.domain.catalog.RubroMaestro;
import com.backset.forze.module.budgeting.infrastructure.ApuComponentRepository;
import com.backset.forze.module.budgeting.infrastructure.ApuMaestroRepository;
import com.backset.forze.module.budgeting.infrastructure.InsumoRepository;
import com.backset.forze.module.budgeting.infrastructure.RubroMaestroRepository;
import com.backset.forze.shared.api.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogService {

	private final InsumoRepository insumoRepository;
	private final RubroMaestroRepository rubroRepository;
	private final ApuMaestroRepository apuRepository;
	private final ApuComponentRepository apuComponentRepository;

	public CatalogService(
			InsumoRepository insumoRepository,
			RubroMaestroRepository rubroRepository,
			ApuMaestroRepository apuRepository,
			ApuComponentRepository apuComponentRepository
	) {
		this.insumoRepository = insumoRepository;
		this.rubroRepository = rubroRepository;
		this.apuRepository = apuRepository;
		this.apuComponentRepository = apuComponentRepository;
	}

	// Insumos
	@Transactional(readOnly = true)
	public List<Insumo> getInsumos(UUID orgId) {
		return insumoRepository.findByOrganizationId(orgId);
	}

	@Transactional(readOnly = true)
	public Insumo getInsumo(UUID orgId, UUID id) {
		Insumo insumo = insumoRepository.findById(id)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Insumo no encontrado."));
		if (!insumo.organizationId().equals(orgId)) {
			throw new ApiException(HttpStatus.FORBIDDEN, "El insumo no pertenece a la organizacion activa.");
		}
		return insumo;
	}

	@Transactional
	public Insumo createInsumo(UUID orgId, CreateInsumoCmd cmd) {
		if (insumoRepository.findByOrganizationIdAndCode(orgId, cmd.code()).isPresent()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Ya existe un insumo con ese codigo.");
		}
		Insumo insumo = new Insumo(UUID.randomUUID(), orgId, cmd.code(), cmd.name(), cmd.unitId(), cmd.type());
		insumo.describe(cmd.description(), cmd.brand(), cmd.specification(), cmd.categoryId());
		if (cmd.referencePrice() != null) {
			insumo.updateReferencePrice(cmd.referencePrice(), cmd.referencePriceCurrency());
		}
		return insumoRepository.save(insumo);
	}

	@Transactional
	public Insumo updateInsumo(UUID orgId, UUID id, CreateInsumoCmd cmd) {
		Insumo insumo = getInsumo(orgId, id);
		insumoRepository.findByOrganizationIdAndCode(orgId, cmd.code())
				.filter(i -> !i.id().equals(id))
				.ifPresent(i -> {
					throw new ApiException(HttpStatus.BAD_REQUEST, "Ya existe otro insumo con ese codigo.");
				});

		Insumo updated = new Insumo(insumo.id(), orgId, cmd.code(), cmd.name(), cmd.unitId(), cmd.type());
		updated.describe(cmd.description(), cmd.brand(), cmd.specification(), cmd.categoryId());
		if (cmd.referencePrice() != null) {
			updated.updateReferencePrice(cmd.referencePrice(), cmd.referencePriceCurrency());
		}
		return insumoRepository.save(updated);
	}

	@Transactional
	public void archiveInsumo(UUID orgId, UUID id) {
		Insumo insumo = getInsumo(orgId, id);
		insumo.archive();
		insumoRepository.save(insumo);
	}

	// APU Maestros
	@Transactional(readOnly = true)
	public List<ApuMaestro> getApuses(UUID orgId) {
		return apuRepository.findByOrganizationId(orgId);
	}

	@Transactional(readOnly = true)
	public ApuMaestro getApu(UUID orgId, UUID id) {
		ApuMaestro apu = apuRepository.findById(id)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "APU no encontrado."));
		if (!apu.organizationId().equals(orgId)) {
			throw new ApiException(HttpStatus.FORBIDDEN, "El APU no pertenece a la organizacion activa.");
		}
		return apu;
	}

	@Transactional
	public ApuMaestro createApu(UUID orgId, CreateApuCmd cmd) {
		UUID id = UUID.randomUUID();
		ApuMaestro apu = new ApuMaestro(id, orgId, cmd.code(), cmd.name(), cmd.unitId(), 1);
		apu.updateEstimate(cmd.yield(), BigDecimal.ZERO, "USD", cmd.validUntil());
		return apuRepository.save(apu);
	}

	@Transactional
	public ApuMaestro updateApu(UUID orgId, UUID id, CreateApuCmd cmd) {
		ApuMaestro apu = getApu(orgId, id);
		apu.updateEstimate(cmd.yield(), apu.estimatedCost(), "USD", cmd.validUntil());
		recalculateApuCostInternal(apu);
		return apuRepository.save(apu);
	}

	@Transactional
	public void archiveApu(UUID orgId, UUID id) {
		ApuMaestro apu = getApu(orgId, id);
		apu.changeStatus(ApuStatus.ARCHIVADO);
		apuRepository.save(apu);
	}

	@Transactional(readOnly = true)
	public List<ApuComponent> getApuComponents(UUID orgId, UUID apuId) {
		getApu(orgId, apuId);
		return apuComponentRepository.findByApuMaestroIdOrderByPosition(apuId);
	}

	@Transactional
	public ApuComponent addComponent(UUID orgId, UUID apuId, AddComponentCmd cmd) {
		ApuMaestro apu = getApu(orgId, apuId);

		List<ApuComponent> existing = apuComponentRepository.findByApuMaestroIdOrderByPosition(apuId);
		int position = existing.isEmpty() ? 1 : existing.get(existing.size() - 1).position() + 1;

		BigDecimal price = cmd.unitPrice();
		if (price == null && cmd.insumoId() != null) {
			Insumo ins = getInsumo(orgId, cmd.insumoId());
			price = ins.referencePrice();
		}
		if (price == null) {
			price = BigDecimal.ZERO;
		}

		ApuComponent component = new ApuComponent(UUID.randomUUID(), apuId, cmd.section(), cmd.unitId(), cmd.quantity(), price, position);
		component.describe(cmd.insumoId(), cmd.description(), cmd.yield(), cmd.wasteFactor());
		ApuComponent saved = apuComponentRepository.save(component);

		recalculateApuCostInternal(apu);
		return saved;
	}

	@Transactional
	public ApuComponent updateComponent(UUID orgId, UUID apuId, UUID componentId, AddComponentCmd cmd) {
		ApuMaestro apu = getApu(orgId, apuId);
		ApuComponent comp = apuComponentRepository.findById(componentId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Componente de APU no encontrado."));
		if (!comp.apuMaestroId().equals(apuId)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "El componente no pertenece al APU especificado.");
		}

		ApuComponent updated = new ApuComponent(componentId, apuId, cmd.section(), cmd.unitId(), cmd.quantity(), cmd.unitPrice(), comp.position());
		updated.describe(cmd.insumoId(), cmd.description(), cmd.yield(), cmd.wasteFactor());
		ApuComponent saved = apuComponentRepository.save(updated);

		recalculateApuCostInternal(apu);
		return saved;
	}

	@Transactional
	public void removeComponent(UUID orgId, UUID apuId, UUID componentId) {
		ApuMaestro apu = getApu(orgId, apuId);
		ApuComponent comp = apuComponentRepository.findById(componentId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Componente de APU no encontrado."));
		if (!comp.apuMaestroId().equals(apuId)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "El componente no pertenece al APU especificado.");
		}
		apuComponentRepository.delete(comp);
		recalculateApuCostInternal(apu);
	}

	private void recalculateApuCostInternal(ApuMaestro apu) {
		List<ApuComponent> comps = apuComponentRepository.findByApuMaestroIdOrderByPosition(apu.id());
		BigDecimal cost = calculateApuCost(apu, comps);
		apu.updateEstimate(apu.yield(), cost, "USD", apu.validUntil());
		apuRepository.save(apu);
	}

	public BigDecimal calculateApuCost(ApuMaestro apu, List<ApuComponent> components) {
		BigDecimal total = BigDecimal.ZERO;
		BigDecimal apuYield = apu.yield() != null && apu.yield().compareTo(BigDecimal.ZERO) > 0 ? apu.yield() : BigDecimal.ONE;

		for (ApuComponent comp : components) {
			BigDecimal quantity = comp.quantity() != null ? comp.quantity() : BigDecimal.ZERO;
			BigDecimal unitPrice = comp.unitPrice() != null ? comp.unitPrice() : BigDecimal.ZERO;
			BigDecimal waste = comp.wasteFactor() != null ? comp.wasteFactor() : BigDecimal.ZERO;
			BigDecimal compYield = comp.yield();

			BigDecimal cost;
			if (comp.section() == ComponentSection.MANO_DE_OBRA || comp.section() == ComponentSection.EQUIPOS) {
				BigDecimal effectiveYield = compYield != null && compYield.compareTo(BigDecimal.ZERO) > 0 ? compYield : apuYield;
				cost = quantity.multiply(unitPrice).multiply(BigDecimal.ONE.add(waste))
						.divide(effectiveYield, 4, RoundingMode.HALF_UP);
			}
			else {
				cost = quantity.multiply(unitPrice).multiply(BigDecimal.ONE.add(waste));
			}
			total = total.add(cost);
		}
		return total.setScale(2, RoundingMode.HALF_UP);
	}

	// Rubro Maestros
	@Transactional(readOnly = true)
	public List<RubroMaestro> getRubros(UUID orgId) {
		return rubroRepository.findByOrganizationId(orgId);
	}

	@Transactional(readOnly = true)
	public RubroMaestro getRubro(UUID orgId, UUID id) {
		RubroMaestro rubro = rubroRepository.findById(id)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Rubro no encontrado."));
		if (!rubro.organizationId().equals(orgId)) {
			throw new ApiException(HttpStatus.FORBIDDEN, "El rubro no pertenece a la organizacion activa.");
		}
		return rubro;
	}

	@Transactional
	public RubroMaestro createRubro(UUID orgId, CreateRubroCmd cmd) {
		if (rubroRepository.findByOrganizationIdAndCode(orgId, cmd.code()).isPresent()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Ya existe un rubro con ese codigo.");
		}
		RubroMaestro rubro = new RubroMaestro(UUID.randomUUID(), orgId, cmd.code(), cmd.name(), cmd.unitId());
		rubro.describe(cmd.description(), cmd.specification(), cmd.keywords(), cmd.categoryId());
		if (cmd.baseApuId() != null) {
			rubro.assignBaseApu(cmd.baseApuId());
		}
		return rubroRepository.save(rubro);
	}

	@Transactional
	public RubroMaestro updateRubro(UUID orgId, UUID id, CreateRubroCmd cmd) {
		RubroMaestro rubro = getRubro(orgId, id);
		rubroRepository.findByOrganizationIdAndCode(orgId, cmd.code())
				.filter(r -> !r.id().equals(id))
				.ifPresent(r -> {
					throw new ApiException(HttpStatus.BAD_REQUEST, "Ya existe otro rubro con ese codigo.");
				});

		RubroMaestro updated = new RubroMaestro(id, orgId, cmd.code(), cmd.name(), cmd.unitId());
		updated.describe(cmd.description(), cmd.specification(), cmd.keywords(), cmd.categoryId());
		if (cmd.baseApuId() != null) {
			updated.assignBaseApu(cmd.baseApuId());
		}
		return rubroRepository.save(updated);
	}

	@Transactional
	public void archiveRubro(UUID orgId, UUID id) {
		RubroMaestro rubro = getRubro(orgId, id);
		rubro.archive();
		rubroRepository.save(rubro);
	}

	// Cmd records
	public record CreateInsumoCmd(
			String code,
			String name,
			String description,
			UUID unitId,
			InsumoType type,
			UUID categoryId,
			String brand,
			String specification,
			BigDecimal referencePrice,
			String referencePriceCurrency
	) {}

	public record CreateApuCmd(
			String code,
			String name,
			UUID unitId,
			BigDecimal yield,
			LocalDate validUntil
	) {}

	public record AddComponentCmd(
			ComponentSection section,
			UUID insumoId,
			String description,
			UUID unitId,
			BigDecimal quantity,
			BigDecimal yield,
			BigDecimal wasteFactor,
			BigDecimal unitPrice
	) {}

	public record CreateRubroCmd(
			String code,
			String name,
			String description,
			UUID unitId,
			UUID categoryId,
			String specification,
			String keywords,
			UUID baseApuId
	) {}
}
