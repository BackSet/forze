import { useQuery } from '@tanstack/react-query'
import { useNavigate } from '@tanstack/react-router'
import { LogOut, RefreshCcw, ShieldCheck } from 'lucide-react'
import { toast } from 'sonner'

import { ThemeToggle } from '@/components/theme-toggle'
import { Button } from '@/components/ui/button'
import { loadCurrentUser, logout } from '@/lib/auth/auth-api'
import { useSessionStore } from '@/lib/auth/session-store'

export function AppPage() {
  const navigate = useNavigate()
  const user = useSessionStore((state) => state.user)
  const refreshing = useSessionStore((state) => state.refreshing)
  const query = useQuery({
    queryKey: ['auth', 'me'],
    queryFn: ({ signal }) => loadCurrentUser(signal),
    retry: false,
  })

  async function handleLogout() {
    await logout()
    toast.success('Sesion cerrada')
    await navigate({ to: '/login' })
  }

  const currentUser = query.data ?? user

  return (
    <main className="min-h-dvh bg-background text-foreground">
      <div className="mx-auto flex min-h-dvh w-full max-w-5xl flex-col px-4 py-4 sm:px-6 lg:px-8">
        <header className="flex h-12 items-center justify-between border-b border-border">
          <a href="/" className="text-sm font-semibold">FORZE</a>
          <div className="flex items-center gap-2">
            <ThemeToggle />
            <Button type="button" variant="outline" onClick={handleLogout}>
              <LogOut aria-hidden="true" />
              Salir
            </Button>
          </div>
        </header>

        <section className="grid flex-1 items-center gap-6 py-10 lg:grid-cols-[minmax(0,1fr)_320px]">
          <div>
            <p className="mb-3 flex items-center gap-2 text-sm font-medium text-primary">
              <ShieldCheck className="size-4" aria-hidden="true" />
              Sesion autenticada
            </p>
            <h1 className="max-w-2xl text-3xl font-semibold leading-tight tracking-normal">
              Base privada lista para construir presupuestos verificables.
            </h1>
            <p className="mt-4 max-w-2xl text-sm leading-6 text-muted-foreground">
              Esta superficie confirma autenticacion, renovacion de access token por refresh cookie HttpOnly y consulta protegida a `/api/auth/me`.
            </p>
          </div>

          <aside className="border border-border bg-panel p-4">
            <h2 className="text-sm font-semibold">Sesion tecnica</h2>
            {query.isLoading ? (
              <div className="mt-4 space-y-2">
                <div className="h-4 w-28 bg-muted" />
                <div className="h-4 w-44 bg-muted" />
              </div>
            ) : null}
            {currentUser ? (
              <dl className="mt-4 space-y-3 text-sm">
                <div>
                  <dt className="text-muted-foreground">Usuario</dt>
                  <dd className="font-medium">{currentUser.username}</dd>
                </div>
                <div>
                  <dt className="text-muted-foreground">ID</dt>
                  <dd className="break-all font-mono text-xs">{currentUser.id}</dd>
                </div>
              </dl>
            ) : null}
            <div className="mt-5 flex items-center gap-2 text-sm text-muted-foreground">
              <RefreshCcw className="size-4" aria-hidden="true" />
              {refreshing ? 'Renovando token' : 'Refresh automatico activo'}
            </div>
          </aside>
        </section>
      </div>
    </main>
  )
}
