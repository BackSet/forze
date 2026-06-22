import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'

import { Providers } from '@/components/providers'
import { applyThemePreference, readStoredTheme } from '@/lib/theme'

import './index.css'

// Apply the persisted theme before render to avoid a flash of the wrong palette.
applyThemePreference(readStoredTheme())
// Keep `system` in sync with OS changes (only relevant while preference is system).
if (typeof window !== 'undefined') {
  window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', () => {
    if (readStoredTheme() === 'system') {
      applyThemePreference('system')
    }
  })
}

const rootElement = document.getElementById('root')

if (!rootElement) {
  throw new Error('Root element not found')
}

createRoot(rootElement).render(
  <StrictMode>
    <Providers />
  </StrictMode>,
)
