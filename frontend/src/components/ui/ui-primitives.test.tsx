import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'

import { StatusBadge } from './status-badge'
import { EmptyState } from './empty-state'
import { PageHeader } from './page-header'

describe('UI primitives', () => {
  it('StatusBadge renders its label (meaning not by color alone)', () => {
    render(<StatusBadge tone="success">Aprobado</StatusBadge>)
    expect(screen.getByText('Aprobado')).toBeInTheDocument()
  })

  it('EmptyState shows title, message and action', () => {
    render(<EmptyState title="Sin datos" message="Nada aún" action={<button>Crear</button>} />)
    expect(screen.getByText('Sin datos')).toBeInTheDocument()
    expect(screen.getByText('Nada aún')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Crear' })).toBeInTheDocument()
  })

  it('PageHeader renders the title as a heading and the actions slot', () => {
    render(<PageHeader title="Proyectos" actions={<button>Nuevo</button>} />)
    expect(screen.getByRole('heading', { name: 'Proyectos' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Nuevo' })).toBeInTheDocument()
  })
})
