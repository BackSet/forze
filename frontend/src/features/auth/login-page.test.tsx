import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'

import { LoginPage } from '@/features/auth/login-page'

vi.mock('@tanstack/react-router', () => ({
  useNavigate: () => vi.fn(),
}))

vi.mock('@/lib/auth/auth-api', () => ({
  login: vi.fn(),
}))

describe('LoginPage', () => {
  it('validates required credentials', async () => {
    render(<LoginPage />)

    await userEvent.click(screen.getByRole('button', { name: /entrar/i }))

    expect(await screen.findByText('Ingresa tu usuario.')).toBeInTheDocument()
    expect(screen.getByText('Ingresa tu contrasena.')).toBeInTheDocument()
  })
})
