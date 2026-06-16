package com.backset.forze.module.budgeting.domain.catalog;

/**
 * Lifecycle of catalog entries (insumos, rubros). Derived from the design "archivar" action
 * and the "estado" field; the design does not enumerate other catalog states.
 */
public enum CatalogStatus {
	ACTIVO,
	ARCHIVADO
}
