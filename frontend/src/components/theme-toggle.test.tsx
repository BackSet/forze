import { render, screen, fireEvent } from '@testing-library/react'
import { beforeEach, describe, expect, it } from 'vitest'

import { ThemeToggle } from './theme-toggle'
import { useSessionStore } from '@/lib/auth/session-store'

describe('ThemeToggle', () => {
  beforeEach(() => {
    window.localStorage.clear()
    delete document.documentElement.dataset.theme
    useSessionStore.getState().setTheme('system')
  })

  it('cycles light → dark → system and persists + applies each preference', () => {
    useSessionStore.getState().setTheme('light')
    render(<ThemeToggle />)
    const button = screen.getByRole('button')

    // light -> dark
    fireEvent.click(button)
    expect(useSessionStore.getState().theme).toBe('dark')
    expect(document.documentElement.dataset.theme).toBe('dark')
    expect(window.localStorage.getItem('forze-theme')).toBe('dark')

    // dark -> system (data-theme removed so prefers-color-scheme drives it)
    fireEvent.click(button)
    expect(useSessionStore.getState().theme).toBe('system')
    expect(document.documentElement.dataset.theme).toBeUndefined()
    expect(window.localStorage.getItem('forze-theme')).toBe('system')

    // system -> light
    fireEvent.click(button)
    expect(useSessionStore.getState().theme).toBe('light')
    expect(document.documentElement.dataset.theme).toBe('light')
  })
})
