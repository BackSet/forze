import { create } from 'zustand'

import type { components } from '@/lib/api/generated/schema'
import { applyThemePreference, readStoredTheme, type ThemePreference } from '@/lib/theme'

export type SessionUser = components['schemas']['MeResponse']

type SessionState = {
  accessToken: string | null
  user: SessionUser | null
  activeOrganizationId: string | null
  // Effective access in the active organization, resolved by the backend.
  role: string | null
  permissions: string[]
  refreshing: boolean
  theme: ThemePreference
  setAccessToken: (accessToken: string | null) => void
  setUser: (user: SessionUser | null) => void
  setActiveOrganizationId: (id: string | null) => void
  setAccess: (role: string | null, permissions: string[]) => void
  setRefreshing: (refreshing: boolean) => void
  setTheme: (theme: ThemePreference) => void
  clearSession: () => void
}

export const useSessionStore = create<SessionState>((set) => ({
  accessToken: null,
  user: null,
  activeOrganizationId: null,
  role: null,
  permissions: [],
  refreshing: false,
  theme: readStoredTheme(),
  setAccessToken: (accessToken) => set({ accessToken }),
  setUser: (user) => set({ user }),
  // Changing organization invalidates the previously resolved access until refetched.
  setActiveOrganizationId: (activeOrganizationId) =>
    set({ activeOrganizationId, role: null, permissions: [] }),
  setAccess: (role, permissions) => set({ role, permissions }),
  setRefreshing: (refreshing) => set({ refreshing }),
  // Persist the preference and apply it to the document immediately.
  setTheme: (theme) => {
    applyThemePreference(theme)
    set({ theme })
  },
  clearSession: () =>
    set({ accessToken: null, user: null, activeOrganizationId: null, role: null, permissions: [], refreshing: false }),
}))
