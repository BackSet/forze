import { render, screen, fireEvent } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'

import { CommandPalette, type PaletteAction } from './command-palette'

function buildActions(run = vi.fn()): PaletteAction[] {
  return [
    { id: 'home', label: 'Ir a Inicio', group: 'Navegación', run },
    { id: 'projects', label: 'Ir a Proyectos', group: 'Navegación', permission: 'PROYECTOS_READ', run },
    { id: 'create-project', label: 'Crear proyecto', group: 'Crear', permission: 'PROYECTOS_WRITE', run },
  ]
}

describe('CommandPalette', () => {
  it('only shows actions the user is permitted to run', () => {
    render(
      <CommandPalette open onOpenChange={() => {}} permissions={['PROYECTOS_READ']} actions={buildActions()} />,
    )

    // No-permission action and a granted-permission action are visible.
    expect(screen.getByText('Ir a Inicio')).toBeInTheDocument()
    expect(screen.getByText('Ir a Proyectos')).toBeInTheDocument()
    // The write action is hidden because the user lacks PROYECTOS_WRITE.
    expect(screen.queryByText('Crear proyecto')).not.toBeInTheDocument()
  })

  it('runs the action and closes when an item is selected', () => {
    const run = vi.fn()
    const onOpenChange = vi.fn()
    render(
      <CommandPalette open onOpenChange={onOpenChange} permissions={[]} actions={buildActions(run)} />,
    )

    fireEvent.click(screen.getByText('Ir a Inicio'))

    expect(run).toHaveBeenCalledTimes(1)
    expect(onOpenChange).toHaveBeenCalledWith(false)
  })

  it('does not render its content when closed', () => {
    render(
      <CommandPalette open={false} onOpenChange={() => {}} permissions={['PROYECTOS_READ']} actions={buildActions()} />,
    )
    expect(screen.queryByText('Ir a Inicio')).not.toBeInTheDocument()
  })
})
