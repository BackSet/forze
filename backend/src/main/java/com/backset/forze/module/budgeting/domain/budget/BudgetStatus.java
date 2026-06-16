package com.backset.forze.module.budgeting.domain.budget;

/**
 * Budget version lifecycle status (design section 7.1).
 */
public enum BudgetStatus {
	BORRADOR,
	EN_CALCULO,
	REQUIERE_AJUSTES,
	PENDIENTE_APROBACION,
	APROBADO,
	ENVIADO,
	ACEPTADO,
	RECHAZADO,
	ARCHIVADO
}
