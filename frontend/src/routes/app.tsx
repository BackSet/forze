import { createFileRoute, redirect } from '@tanstack/react-router'

import { AppPage } from '@/app/app-page'
import { loadCurrentUser } from '@/lib/auth/auth-api'

export const Route = createFileRoute('/app')({
  beforeLoad: async () => {
    try {
      await loadCurrentUser()
    }
    catch {
      throw redirect({ to: '/login' })
    }
  },
  component: AppPage,
})
