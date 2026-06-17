import { create } from 'zustand'

import type { components } from '@/lib/api/generated/schema'

export type SessionUser = components['schemas']['MeResponse']

type ThemePreference = 'light' | 'dark' | 'system'

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
  theme: 'system',
  setAccessToken: (accessToken) => set({ accessToken }),
  setUser: (user) => set({ user }),
  // Changing organization invalidates the previously resolved access until refetched.
  setActiveOrganizationId: (activeOrganizationId) =>
    set({ activeOrganizationId, role: null, permissions: [] }),
  setAccess: (role, permissions) => set({ role, permissions }),
  setRefreshing: (refreshing) => set({ refreshing }),
  setTheme: (theme) => set({ theme }),
  clearSession: () =>
    set({ accessToken: null, user: null, activeOrganizationId: null, role: null, permissions: [], refreshing: false }),
}))
