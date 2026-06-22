import { Monitor, Moon, Sun } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { useSessionStore } from '@/lib/auth/session-store'
import { type ThemePreference } from '@/lib/theme'

const ORDER: ThemePreference[] = ['light', 'dark', 'system']

const META: Record<ThemePreference, { label: string; icon: typeof Sun }> = {
  light: { label: 'Tema claro', icon: Sun },
  dark: { label: 'Tema oscuro', icon: Moon },
  system: { label: 'Tema del sistema', icon: Monitor },
}

/**
 * Cycles light → dark → system. The preference is persisted (via the session
 * store) and applied to the document; `system` follows `prefers-color-scheme`.
 * The current mode is conveyed by both icon and accessible label (not color).
 */
export function ThemeToggle() {
  const theme = useSessionStore((s) => s.theme)
  const setTheme = useSessionStore((s) => s.setTheme)

  const { label, icon: Icon } = META[theme]
  const next = ORDER[(ORDER.indexOf(theme) + 1) % ORDER.length] ?? 'system'

  return (
    <Button
      type="button"
      variant="ghost"
      size="icon"
      aria-label={`${label} (cambiar a ${META[next].label.toLowerCase()})`}
      title={label}
      onClick={() => setTheme(next)}
    >
      <Icon aria-hidden="true" />
    </Button>
  )
}
