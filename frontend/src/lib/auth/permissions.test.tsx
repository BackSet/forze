import { render, screen } from '@testing-library/react'
import { renderHook } from '@testing-library/react'
import { beforeEach, describe, expect, it } from 'vitest'

import { PermissionGate } from '@/lib/auth/permission-gate'
import { usePermission, useIsAdministrator } from '@/lib/auth/permissions'
import { useSessionStore } from '@/lib/auth/session-store'

describe('permission gating', () => {
  beforeEach(() => {
    useSessionStore.getState().clearSession()
  })

  it('usePermission reflects the effective permissions in the session', () => {
    useSessionStore.getState().setAccess('PRESUPUESTISTA', ['PROYECTOS_READ', 'PRESUPUESTOS_READ'])

    expect(renderHook(() => usePermission('PROYECTOS_READ')).result.current).toBe(true)
    expect(renderHook(() => usePermission('ADMINISTRACION_WRITE')).result.current).toBe(false)
  })

  it('useIsAdministrator is true only for the ADMINISTRADOR role', () => {
    useSessionStore.getState().setAccess('ADMINISTRADOR', ['ADMINISTRACION_WRITE'])
    expect(renderHook(() => useIsAdministrator()).result.current).toBe(true)
  })

  it('PermissionGate renders children only when permitted, else the fallback', () => {
    useSessionStore.getState().setAccess('COMPRAS', ['PROVEEDORES_WRITE'])

    render(
      <PermissionGate permission="PROVEEDORES_WRITE" fallback={<span>denied</span>}>
        <span>allowed</span>
      </PermissionGate>,
    )
    expect(screen.getByText('allowed')).toBeInTheDocument()

    render(
      <PermissionGate permission="ADMINISTRACION_WRITE" fallback={<span>denied</span>}>
        <span>secret</span>
      </PermissionGate>,
    )
    expect(screen.getByText('denied')).toBeInTheDocument()
    expect(screen.queryByText('secret')).not.toBeInTheDocument()
  })

  it('changing organization clears previously resolved permissions', () => {
    useSessionStore.getState().setAccess('ADMINISTRADOR', ['ADMINISTRACION_WRITE'])
    useSessionStore.getState().setActiveOrganizationId('00000000-0000-0000-0000-000000000001')
    expect(useSessionStore.getState().permissions).toEqual([])
    expect(useSessionStore.getState().role).toBeNull()
  })
})
