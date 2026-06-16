package com.backset.forze.module.budgeting.domain.approval;

/**
 * Approval request status (design section 15). The pre-submit BORRADOR state lives on the budget
 * version, not on the approval record.
 */
public enum ApprovalStatus {
	PENDIENTE_APROBACION,
	OBSERVADO,
	APROBADO,
	RECHAZADO
}
