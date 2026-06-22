import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'

import { CodeField } from './code-field'

describe('CodeField', () => {
  it('fills the value from the generator', async () => {
    const onChange = vi.fn()
    const onGenerate = vi.fn().mockResolvedValue('PRY-2026-0007')
    render(<CodeField value="" onChange={onChange} onGenerate={onGenerate} />)

    fireEvent.click(screen.getByRole('button', { name: /Generar código sugerido/i }))

    await waitFor(() => expect(onChange).toHaveBeenCalledWith('PRY-2026-0007'))
  })

  it('supports manual entry and hides the generate button without a generator', () => {
    const onChange = vi.fn()
    render(<CodeField value="" onChange={onChange} />)

    fireEvent.change(screen.getByLabelText('Código'), { target: { value: 'MANUAL-1' } })
    expect(onChange).toHaveBeenCalledWith('MANUAL-1')
    expect(screen.queryByRole('button', { name: /Generar/i })).not.toBeInTheDocument()
  })
})
