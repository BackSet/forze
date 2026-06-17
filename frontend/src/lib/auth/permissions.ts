import { useEffect } from 'react'
import { useQuery } from '@tanstack/react-query'

import { api } from '@/lib/api/client'
import { useSessionStore } from '@/lib/auth/session-store'

/**
 * Permission codes mirror the backend `ForzePermission` enum. These are UI
 * gating hints only — the backend `@PreAuthorize` checks remain the authority.
 */
export type Permission =
  | 'PROYECTOS_READ' | 'PROYECTOS_WRITE'
  | 'PRESUPUESTOS_READ' | 'PRESUPUESTOS_WRITE'
  | 'CATALOGOS_READ' | 'CATALOGOS_WRITE'
  | 'PROVEEDORES_READ' | 'PROVEEDORES_WRITE'
  | 'APROBACIONES_READ' | 'APROBACIONES_WRITE'
  | 'DOCUMENTOS_READ' | 'DOCUMENTOS_WRITE'
  | 'ADMINISTRACION_READ' | 'ADMINISTRACION_WRITE'
  | 'AUDITORIA_READ'

export function usePermission(permission: Permission): boolean {
  return useSessionStore((s) => s.permissions.includes(permission))
}

export function useHasAnyPermission(permissions: Permission[]): boolean {
  return useSessionStore((s) => permissions.some((p) => s.permissions.includes(p)))
}

export function useIsAdministrator(): boolean {
  return useSessionStore((s) => s.role === 'ADMINISTRADOR')
}

/**
 * Loads the current user's effective role and permissions for the active
 * organization and keeps the session store in sync. Re-runs whenever the
 * active organization changes (the request carries X-Organization-Id).
 */
export function useEffectiveAccess() {
  const activeOrganizationId = useSessionStore((s) => s.activeOrganizationId)
  const setAccess = useSessionStore((s) => s.setAccess)

  const query = useQuery({
    ...api.queryOptions('get', '/api/me/access'),
    enabled: !!activeOrganizationId,
  })

  useEffect(() => {
    if (query.data) {
      setAccess(query.data.role ?? null, query.data.permissions ?? [])
    }
  }, [query.data, setAccess])

  return query
}
