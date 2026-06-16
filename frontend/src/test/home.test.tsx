import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'

import { HomePage } from '@/components/home-page'

describe('HomePage', () => {
  it('renders the FORZE technical foundation screen', () => {
    render(<HomePage />)

    expect(screen.getByText('FORZE')).toBeInTheDocument()
    expect(screen.getByText('Base tecnica inicializada')).toBeInTheDocument()
  })
})
