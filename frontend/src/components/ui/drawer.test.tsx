import { render, screen, fireEvent } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'

import { Drawer } from './drawer'

describe('Drawer', () => {
  it('renders content when open and nothing when closed', () => {
    const { rerender } = render(
      <Drawer open onClose={() => {}} title="Navegación">
        <span>contenido</span>
      </Drawer>,
    )
    expect(screen.getByRole('dialog', { name: 'Navegación' })).toBeInTheDocument()
    expect(screen.getByText('contenido')).toBeInTheDocument()

    rerender(
      <Drawer open={false} onClose={() => {}} title="Navegación">
        <span>contenido</span>
      </Drawer>,
    )
    expect(screen.queryByText('contenido')).not.toBeInTheDocument()
  })

  it('closes on Escape', () => {
    const onClose = vi.fn()
    render(
      <Drawer open onClose={onClose} title="Navegación">
        <span>contenido</span>
      </Drawer>,
    )
    fireEvent.keyDown(document, { key: 'Escape' })
    expect(onClose).toHaveBeenCalled()
  })
})
