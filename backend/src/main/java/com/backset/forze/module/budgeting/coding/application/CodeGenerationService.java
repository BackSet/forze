package com.backset.forze.module.budgeting.coding.application;

import java.time.LocalDate;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.UUID;

import com.backset.forze.module.budgeting.domain.budget.Budget;
import com.backset.forze.module.budgeting.domain.catalog.ApuMaestro;
import com.backset.forze.module.budgeting.domain.catalog.Insumo;
import com.backset.forze.module.budgeting.domain.catalog.RubroMaestro;
import com.backset.forze.module.budgeting.domain.project.Project;
import com.backset.forze.module.budgeting.infrastructure.ApuMaestroRepository;
import com.backset.forze.module.budgeting.infrastructure.BudgetRepository;
import com.backset.forze.module.budgeting.infrastructure.InsumoRepository;
import com.backset.forze.module.budgeting.infrastructure.ProjectRepository;
import com.backset.forze.module.budgeting.infrastructure.RubroMaestroRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Suggests the next sequential business code for the main entities. The backend
 * is the source of truth so the UI never has to guess a unique code.
 *
 * <p>Codes are <em>suggested</em>, not reserved: the value is computed from the
 * existing codes in scope and returned without persisting anything. Final
 * uniqueness is guaranteed by the per-scope unique constraints at save time (a
 * concurrent collision surfaces as a clear conflict error), which keeps this
 * read-only and free of locking.
 *
 * <p>Formats (see {@code docs/ai/NAMING.md}):
 * project {@code PRY-YYYY-0001}, budget {@code PRE-YYYY-0001} (unique per
 * project), insumo {@code INS-0001}, APU {@code APU-0001}, rubro {@code RUB-0001}
 * (the last three unique per organization). Codes that do not match the active
 * pattern (e.g. manually typed or demo codes) are ignored when computing the
 * next sequence.
 */
@Service
@Transactional(readOnly = true)
public class CodeGenerationService {

	private final ProjectRepository projectRepository;
	private final BudgetRepository budgetRepository;
	private final InsumoRepository insumoRepository;
	private final ApuMaestroRepository apuRepository;
	private final RubroMaestroRepository rubroRepository;

	public CodeGenerationService(ProjectRepository projectRepository, BudgetRepository budgetRepository,
			InsumoRepository insumoRepository, ApuMaestroRepository apuRepository, RubroMaestroRepository rubroRepository) {
		this.projectRepository = projectRepository;
		this.budgetRepository = budgetRepository;
		this.insumoRepository = insumoRepository;
		this.apuRepository = apuRepository;
		this.rubroRepository = rubroRepository;
	}

	public String nextProjectCode(UUID organizationId) {
		String prefix = "PRY-" + LocalDate.now().getYear() + "-";
		return next(prefix, 4, projectRepository.findByOrganizationId(organizationId).stream().map(Project::code).toList());
	}

	public String nextBudgetCode(UUID organizationId, UUID projectId) {
		// Budget codes are unique per project (uq_budgeting_budgets_project_code).
		String prefix = "PRE-" + LocalDate.now().getYear() + "-";
		return next(prefix, 4, budgetRepository.findByProjectId(projectId).stream().map(Budget::code).toList());
	}

	public String nextInsumoCode(UUID organizationId) {
		return next("INS-", 4, insumoRepository.findByOrganizationId(organizationId).stream().map(Insumo::code).toList());
	}

	public String nextApuCode(UUID organizationId) {
		return next("APU-", 4, apuRepository.findByOrganizationId(organizationId).stream().map(ApuMaestro::code).toList());
	}

	public String nextRubroCode(UUID organizationId) {
		return next("RUB-", 4, rubroRepository.findByOrganizationId(organizationId).stream().map(RubroMaestro::code).toList());
	}

	/**
	 * Computes {@code prefix + zero-padded(maxExistingSequence + 1)} for the given
	 * prefix, considering only codes that match {@code ^<prefix>\d+$}.
	 */
	private String next(String prefix, int width, Collection<String> existingCodes) {
		Pattern pattern = Pattern.compile("^" + Pattern.quote(prefix) + "(\\d+)$");
		long max = 0;
		for (String code : existingCodes) {
			if (code == null) {
				continue;
			}
			Matcher matcher = pattern.matcher(code);
			if (matcher.matches()) {
				try {
					max = Math.max(max, Long.parseLong(matcher.group(1)));
				}
				catch (NumberFormatException ignored) {
					// Out-of-range numeric suffix: ignore, keep scanning.
				}
			}
		}
		return prefix + String.format("%0" + width + "d", max + 1);
	}
}
