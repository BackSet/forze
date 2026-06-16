package com.backset.forze.module.budgeting.domain.budget;

/**
 * Budget item validation status (design section 9.1 "estado de validacion"). Derived from the
 * alerts "rubros incompletos" / "rubros obligatorios completos" (sections 5 and 14).
 */
public enum ItemValidationStatus {
	COMPLETO,
	INCOMPLETO
}
