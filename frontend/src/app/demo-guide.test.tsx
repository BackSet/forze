import { render, screen, fireEvent } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'

import { DemoGuide } from './demo-guide'
import { DEMO_PASSWORD } from '@/lib/env'

describe('DemoGuide', () => {
  it('shows an actionable empty state when the demo data is not loaded', () => {
    const onNavigate = vi.fn()
    render(<DemoGuide demoLoaded={false} loading={false} onNavigate={onNavigate} />)

    expect(screen.getByText(/no están cargados/i)).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: /Ir a Proyectos/i }))
    expect(onNavigate).toHaveBeenCalledWith('projects')
  })

  it('shows credentials and tour shortcuts when loaded, and navigates on click', () => {
    const onNavigate = vi.fn()
    render(<DemoGuide demoLoaded loading={false} onNavigate={onNavigate} />)

    // Local demo password is surfaced for convenience.
    expect(screen.getByText(DEMO_PASSWORD)).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: /Presupuestos/i }))
    expect(onNavigate).toHaveBeenCalledWith('budgets')

    fireEvent.click(screen.getByRole('button', { name: /Escenarios/i }))
    expect(onNavigate).toHaveBeenCalledWith('scenarios')
  })
})
