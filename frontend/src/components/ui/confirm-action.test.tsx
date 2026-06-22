import { render, screen, fireEvent } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'

import { ConfirmAction } from './confirm-action'

describe('ConfirmAction', () => {
  it('runs onConfirm only after explicit confirmation', () => {
    const onConfirm = vi.fn()
    render(
      <ConfirmAction triggerLabel="Eliminar" onConfirm={onConfirm}>
        x
      </ConfirmAction>,
    )

    // No confirmation shown yet, nothing fired.
    expect(onConfirm).not.toHaveBeenCalled()

    fireEvent.click(screen.getByRole('button', { name: 'Eliminar' }))
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar' }))
    expect(onConfirm).toHaveBeenCalledTimes(1)
  })

  it('does not run onConfirm when cancelled', () => {
    const onConfirm = vi.fn()
    render(
      <ConfirmAction triggerLabel="Eliminar" onConfirm={onConfirm}>
        x
      </ConfirmAction>,
    )

    fireEvent.click(screen.getByRole('button', { name: 'Eliminar' }))
    fireEvent.click(screen.getByRole('button', { name: 'Cancelar' }))
    expect(onConfirm).not.toHaveBeenCalled()
  })
})
