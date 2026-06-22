export type ThemePreference = 'light' | 'dark' | 'system'

const THEME_KEY = 'forze-theme'

/** Reads the persisted preference, defaulting to `system`. */
export function readStoredTheme(): ThemePreference {
  if (typeof window === 'undefined') {
    return 'system'
  }
  const stored = window.localStorage.getItem(THEME_KEY)
  return stored === 'light' || stored === 'dark' || stored === 'system' ? stored : 'system'
}

/** True when the OS currently prefers a dark color scheme. */
export function systemPrefersDark(): boolean {
  return typeof window !== 'undefined' && window.matchMedia('(prefers-color-scheme: dark)').matches
}

/** Resolves a preference to the effective light/dark actually shown. */
export function resolveEffectiveTheme(preference: ThemePreference): 'light' | 'dark' {
  if (preference === 'system') {
    return systemPrefersDark() ? 'dark' : 'light'
  }
  return preference
}

/**
 * Applies the preference to the document and persists it. `light`/`dark` set
 * `data-theme` explicitly; `system` removes it so the `prefers-color-scheme`
 * media query in index.css drives the palette.
 */
export function applyThemePreference(preference: ThemePreference): void {
  if (typeof document === 'undefined') {
    return
  }
  const root = document.documentElement
  if (preference === 'system') {
    delete root.dataset.theme
  }
  else {
    root.dataset.theme = preference
  }
  if (typeof window !== 'undefined') {
    window.localStorage.setItem(THEME_KEY, preference)
  }
}
