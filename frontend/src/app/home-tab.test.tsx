import { render, screen } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { describe, expect, it, vi, beforeEach } from 'vitest'

import { HomeTab } from './home-tab'
import { useSessionStore } from '@/lib/auth/session-store'

function renderHome(permissions: string[]) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <HomeTab permissions={permissions} onNavigate={vi.fn()} onOpenPalette={vi.fn()} />
    </QueryClientProvider>,
  )
}

describe('HomeTab', () => {
  beforeEach(() => {
    useSessionStore.getState().clearSession()
  })

  it('shows only the KPI cards the user can read', () => {
    renderHome(['PROYECTOS_READ'])

    expect(screen.getByText('Proyectos activos')).toBeInTheDocument()
    // Catalog/suppliers/members cards require their own read permissions.
    expect(screen.queryByText('Insumos en catálogo')).not.toBeInTheDocument()
    expect(screen.queryByText('Proveedores')).not.toBeInTheDocument()
    expect(screen.queryByText('Miembros')).not.toBeInTheDocument()
  })

  it('exposes quick-create only for write permissions held', () => {
    renderHome(['PROYECTOS_READ', 'PROYECTOS_WRITE'])

    expect(screen.getByText('Creación rápida')).toBeInTheDocument()
    expect(screen.getByText('Crear proyecto')).toBeInTheDocument()
    expect(screen.queryByText('Nuevo presupuesto')).not.toBeInTheDocument()
  })

  it('hides recent activity without audit permission and always shows an alerts pointer', () => {
    renderHome([])

    expect(screen.queryByText('Actividad reciente')).not.toBeInTheDocument()
    // Critical alerts panel is always present as an actionable, source-honest pointer.
    expect(screen.getByText('Alertas críticas')).toBeInTheDocument()
    expect(
      screen.getByText(/Las alertas de calidad y viabilidad se evalúan por versión/),
    ).toBeInTheDocument()
  })
})
