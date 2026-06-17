package com.backset.forze.module.budgeting.budget.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.backset.forze.module.budgeting.domain.budget.BudgetItem;
import com.backset.forze.module.budgeting.domain.budget.BudgetVersion;
import com.backset.forze.module.budgeting.domain.budget.ItemApu;
import com.backset.forze.module.budgeting.domain.budget.ItemApuComponent;
import com.backset.forze.module.budgeting.domain.budget.Measurement;
import com.backset.forze.module.budgeting.domain.budget.BudgetRisk;
import com.backset.forze.module.budgeting.domain.supplier.PriceHistory;
import com.backset.forze.module.budgeting.domain.supplier.PriceStatus;
import com.backset.forze.module.budgeting.domain.catalog.ApuMaestro;
import com.backset.forze.module.budgeting.infrastructure.ItemApuRepository;
import com.backset.forze.module.budgeting.infrastructure.ItemApuComponentRepository;
import com.backset.forze.module.budgeting.infrastructure.PriceHistoryRepository;
import com.backset.forze.module.budgeting.infrastructure.BudgetRiskRepository;
import com.backset.forze.module.budgeting.infrastructure.MeasurementRepository;
import com.backset.forze.module.budgeting.infrastructure.ApuMaestroRepository;
import org.springframework.stereotype.Service;

@Service
public class AlertGenerationService {

	private final ItemApuRepository apuRepository;
	private final ItemApuComponentRepository componentRepository;
	private final PriceHistoryRepository priceHistoryRepository;
	private final BudgetRiskRepository riskRepository;
	private final MeasurementRepository measurementRepository;
	private final ApuMaestroRepository apuMaestroRepository;

	public AlertGenerationService(
			ItemApuRepository apuRepository,
			ItemApuComponentRepository componentRepository,
			PriceHistoryRepository priceHistoryRepository,
			BudgetRiskRepository riskRepository,
			MeasurementRepository measurementRepository,
			ApuMaestroRepository apuMaestroRepository
	) {
		this.apuRepository = apuRepository;
		this.componentRepository = componentRepository;
		this.priceHistoryRepository = priceHistoryRepository;
		this.riskRepository = riskRepository;
		this.measurementRepository = measurementRepository;
		this.apuMaestroRepository = apuMaestroRepository;
	}

	public List<BudgetAlert> generateAlerts(
			BudgetVersion version,
			List<BudgetItem> items,
			BigDecimal minimumMargin
	) {
		List<BudgetAlert> alerts = new ArrayList<>();
		UUID orgIdActive = com.backset.forze.shared.TenantContext.getTenantId();

		BigDecimal totalCost = version.totalCost() != null ? version.totalCost() : BigDecimal.ZERO;
		BigDecimal salePrice = version.salePrice() != null ? version.salePrice() : BigDecimal.ZERO;
		BigDecimal margin = version.margin() != null ? version.margin() : BigDecimal.ZERO;
		BigDecimal targetAmount = version.targetAmount();

		if (totalCost.compareTo(salePrice) >= 0 && salePrice.compareTo(BigDecimal.ZERO) > 0) {
			alerts.add(new BudgetAlert("totalCost", "El costo interno es mayor o igual al precio ofertado."));
		}

		if (targetAmount != null && targetAmount.compareTo(BigDecimal.ZERO) > 0) {
			if (salePrice.compareTo(targetAmount) > 0) {
				alerts.add(new BudgetAlert("salePrice", "El precio ofertado supera el monto objetivo."));
			}
		}

		if (minimumMargin != null && margin.compareTo(minimumMargin) < 0) {
			alerts.add(new BudgetAlert("margin", "El margen es inferior al minimo requerido (" + minimumMargin.multiply(new BigDecimal("100")).setScale(2) + "%)."));
		}

		LocalDate today = LocalDate.now();

		for (BudgetItem item : items) {
			if (item.quantity() == null || item.quantity().compareTo(BigDecimal.ZERO) == 0) {
				alerts.add(new BudgetAlert("item:" + item.id(), "El rubro '" + item.name() + "' tiene cantidad cero."));
			}
			if (item.validationStatus() == com.backset.forze.module.budgeting.domain.budget.ItemValidationStatus.INCOMPLETO) {
				alerts.add(new BudgetAlert("item:" + item.id(), "El rubro '" + item.name() + "' esta incompleto."));
			}

			// Rubros sin APU
			ItemApu apu = apuRepository.findByBudgetItemId(item.id()).orElse(null);
			if (apu == null) {
				alerts.add(new BudgetAlert("item:" + item.id(), "El rubro '" + item.name() + "' no tiene un APU configurado."));
			} else {
				List<ItemApuComponent> components = componentRepository.findByItemApuIdOrderByPosition(apu.id());
				if (components.isEmpty()) {
					alerts.add(new BudgetAlert("item:" + item.id(), "El APU del rubro '" + item.name() + "' no tiene componentes."));
				} else {
					for (ItemApuComponent comp : components) {
						// Precios sin fuente
						if (comp.priceSource() == null || comp.priceSource().isBlank()) {
							alerts.add(new BudgetAlert("component:" + comp.id(), "El componente '" + comp.description() + "' del rubro '" + item.name() + "' no tiene una fuente de precio registrada."));
						}

						// Precios vencidos
						if (comp.sourceInsumoId() != null && orgIdActive != null) {
							PriceHistory latestPrice = priceHistoryRepository.findByInsumoIdOrderByPriceDateDesc(comp.sourceInsumoId()).stream()
									.filter(ph -> ph.organizationId().equals(orgIdActive) && ph.status() == PriceStatus.VIGENTE)
									.findFirst()
									.orElse(null);

							if (latestPrice != null) {
								if (latestPrice.status() == PriceStatus.VENCIDO || (latestPrice.validUntil() != null && latestPrice.validUntil().isBefore(today))) {
									alerts.add(new BudgetAlert("component:" + comp.id(), "El precio del insumo '" + comp.description() + "' en el rubro '" + item.name() + "' está vencido."));
								}
							}
						}
					}
				}

				// Rendimientos no verificados
				if (apu.yield() == null || apu.yield().compareTo(BigDecimal.ZERO) <= 0) {
					alerts.add(new BudgetAlert("apu:" + apu.id(), "El rendimiento del APU para el rubro '" + item.name() + "' no está verificado (cero o nulo)."));
				}
				if (apu.sourceApuId() != null) {
					ApuMaestro masterApu = apuMaestroRepository.findById(apu.sourceApuId()).orElse(null);
					if (masterApu != null && "BORRADOR".equals(masterApu.status().name())) {
						alerts.add(new BudgetAlert("apu:" + apu.id(), "El rendimiento del APU para el rubro '" + item.name() + "' no está verificado (APU maestro en BORRADOR)."));
					}
				}
			}

			// Cantidades sin medición
			if (item.quantity() != null && item.quantity().compareTo(BigDecimal.ZERO) > 0) {
				List<Measurement> measurements = measurementRepository.findByBudgetItemIdOrderByPosition(item.id());
				if (measurements.isEmpty()) {
					alerts.add(new BudgetAlert("item:" + item.id(), "El rubro '" + item.name() + "' no tiene líneas de medición registradas."));
				} else {
					BigDecimal sum = measurements.stream()
							.map(m -> m.result() != null ? m.result() : BigDecimal.ZERO)
							.reduce(BigDecimal.ZERO, BigDecimal::add);
					if (sum.subtract(item.quantity()).abs().compareTo(new BigDecimal("0.01")) > 0) {
						alerts.add(new BudgetAlert("item:" + item.id(), "La cantidad del rubro '" + item.name() + "' (" + item.quantity() + ") no coincide con la suma de sus mediciones (" + sum + ")."));
					}
				}
			}
		}

		// Riesgos sin mitigar
		List<BudgetRisk> risks = riskRepository.findByBudgetVersionId(version.id());
		for (BudgetRisk risk : risks) {
			if (!risk.mitigated() || risk.mitigation() == null || risk.mitigation().isBlank()) {
				alerts.add(new BudgetAlert("risk:" + risk.id(), "El riesgo '" + risk.description() + "' no está mitigado."));
			}
		}

		return alerts;
	}

	public int calculateQualityScore(BudgetVersion version, List<BudgetItem> items) {
		int score = 100;
		UUID orgIdActive = com.backset.forze.shared.TenantContext.getTenantId();
		LocalDate today = LocalDate.now();

		// 1. Rubros sin APU (max penalty -40)
		int noApuCount = 0;
		for (BudgetItem item : items) {
			ItemApu apu = apuRepository.findByBudgetItemId(item.id()).orElse(null);
			if (apu == null) {
				noApuCount++;
			} else {
				List<ItemApuComponent> components = componentRepository.findByItemApuIdOrderByPosition(apu.id());
				if (components.isEmpty()) {
					noApuCount++;
				}
			}
		}
		score -= Math.min(noApuCount * 10, 40);

		// 2. Precios vencidos (max penalty -20)
		int expiredPricesCount = 0;
		for (BudgetItem item : items) {
			ItemApu apu = apuRepository.findByBudgetItemId(item.id()).orElse(null);
			if (apu != null) {
				List<ItemApuComponent> components = componentRepository.findByItemApuIdOrderByPosition(apu.id());
				for (ItemApuComponent comp : components) {
					if (comp.sourceInsumoId() != null && orgIdActive != null) {
						PriceHistory latestPrice = priceHistoryRepository.findByInsumoIdOrderByPriceDateDesc(comp.sourceInsumoId()).stream()
								.filter(ph -> ph.organizationId().equals(orgIdActive) && ph.status() == PriceStatus.VIGENTE)
								.findFirst()
								.orElse(null);
						if (latestPrice != null && (latestPrice.status() == PriceStatus.VENCIDO || (latestPrice.validUntil() != null && latestPrice.validUntil().isBefore(today)))) {
							expiredPricesCount++;
						}
					}
				}
			}
		}
		score -= Math.min(expiredPricesCount * 5, 20);

		// 3. Precios sin fuente (max penalty -20)
		int noSourceCount = 0;
		for (BudgetItem item : items) {
			ItemApu apu = apuRepository.findByBudgetItemId(item.id()).orElse(null);
			if (apu != null) {
				List<ItemApuComponent> components = componentRepository.findByItemApuIdOrderByPosition(apu.id());
				for (ItemApuComponent comp : components) {
					if (comp.priceSource() == null || comp.priceSource().isBlank()) {
						noSourceCount++;
					}
				}
			}
		}
		score -= Math.min(noSourceCount * 5, 20);

		// 4. Cantidades sin medición (max penalty -20)
		int noMeasurementCount = 0;
		for (BudgetItem item : items) {
			if (item.quantity() != null && item.quantity().compareTo(BigDecimal.ZERO) > 0) {
				List<Measurement> measurements = measurementRepository.findByBudgetItemIdOrderByPosition(item.id());
				if (measurements.isEmpty()) {
					noMeasurementCount++;
				} else {
					BigDecimal sum = measurements.stream()
							.map(m -> m.result() != null ? m.result() : BigDecimal.ZERO)
							.reduce(BigDecimal.ZERO, BigDecimal::add);
					if (sum.subtract(item.quantity()).abs().compareTo(new BigDecimal("0.01")) > 0) {
						noMeasurementCount++;
					}
				}
			}
		}
		score -= Math.min(noMeasurementCount * 5, 20);

		// 5. Rendimientos no verificado (max penalty -20)
		int unverifiedYieldCount = 0;
		for (BudgetItem item : items) {
			ItemApu apu = apuRepository.findByBudgetItemId(item.id()).orElse(null);
			if (apu != null) {
				if (apu.yield() == null || apu.yield().compareTo(BigDecimal.ZERO) <= 0) {
					unverifiedYieldCount++;
				} else if (apu.sourceApuId() != null) {
					ApuMaestro masterApu = apuMaestroRepository.findById(apu.sourceApuId()).orElse(null);
					if (masterApu != null && "BORRADOR".equals(masterApu.status().name())) {
						unverifiedYieldCount++;
					}
				}
			}
		}
		score -= Math.min(unverifiedYieldCount * 5, 20);

		// 6. Riesgos sin mitigar (max penalty -30)
		List<BudgetRisk> risks = riskRepository.findByBudgetVersionId(version.id());
		int unmitigatedRisks = 0;
		for (BudgetRisk risk : risks) {
			if (!risk.mitigated() || risk.mitigation() == null || risk.mitigation().isBlank()) {
				unmitigatedRisks++;
			}
		}
		score -= Math.min(unmitigatedRisks * 10, 30);

		return Math.max(0, Math.min(score, 100));
	}

	public record BudgetAlert(String field, String message) {}
}
