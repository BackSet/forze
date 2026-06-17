import { zodResolver } from '@hookform/resolvers/zod'
import { useNavigate } from '@tanstack/react-router'
import { Loader2 } from 'lucide-react'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { toast } from 'sonner'
import { z } from 'zod'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { ThemeToggle } from '@/components/theme-toggle'
import { normalizeApiError } from '@/lib/api/errors'
import { login } from '@/lib/auth/auth-api'

const loginSchema = z.object({
  username: z.string().min(1, 'Ingresa tu usuario.'),
  password: z.string().min(1, 'Ingresa tu contrasena.'),
})

type LoginForm = z.infer<typeof loginSchema>

export function LoginPage() {
  const navigate = useNavigate()
  const [serverError, setServerError] = useState<string | null>(null)
  const form = useForm<LoginForm>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      username: '',
      password: '',
    },
  })

  async function onSubmit(values: LoginForm) {
    setServerError(null)
    try {
      await login(values)
      toast.success('Sesion autenticada')
      await navigate({ to: '/app' })
    }
    catch (error) {
      const normalized = normalizeApiError(error)
      setServerError(normalized.detail)
      toast.error('No se pudo iniciar sesion', {
        description: normalized.detail,
      })
    }
  }

  const disabled = form.formState.isSubmitting

  return (
    <main className="min-h-dvh bg-background text-foreground">
      <div className="mx-auto flex min-h-dvh w-full max-w-md flex-col px-4 py-4 sm:px-6">
        <header className="flex h-12 items-center justify-between border-b border-border">
          <a href="/" className="text-sm font-semibold">FORZE</a>
          <ThemeToggle />
        </header>

        <section className="flex flex-1 items-center py-10">
          <div className="w-full border border-border bg-panel p-5">
            <h1 className="text-xl font-semibold">Acceso tecnico</h1>
            <p className="mt-2 text-sm leading-6 text-muted-foreground">
              Inicia sesion para entrar al espacio autenticado de FORZE.
            </p>

            <form className="mt-6 space-y-4" onSubmit={form.handleSubmit(onSubmit)} noValidate>
              <div>
                <label className="text-sm font-medium" htmlFor="username">Usuario</label>
                <Input
                  id="username"
                  autoComplete="username"
                  invalid={Boolean(form.formState.errors.username)}
                  disabled={disabled}
                  {...form.register('username')}
                />
                {form.formState.errors.username ? (
                  <p className="mt-1 text-sm text-destructive">{form.formState.errors.username.message}</p>
                ) : null}
              </div>

              <div>
                <label className="text-sm font-medium" htmlFor="password">Contrasena</label>
                <Input
                  id="password"
                  type="password"
                  autoComplete="current-password"
                  invalid={Boolean(form.formState.errors.password)}
                  disabled={disabled}
                  {...form.register('password')}
                />
                {form.formState.errors.password ? (
                  <p className="mt-1 text-sm text-destructive">{form.formState.errors.password.message}</p>
                ) : null}
              </div>

              {serverError ? (
                <p className="border border-destructive/40 bg-background p-3 text-sm text-destructive" role="alert">
                  {serverError}
                </p>
              ) : null}

              <Button className="w-full" disabled={disabled} type="submit">
                {disabled ? <Loader2 className="animate-spin" aria-hidden="true" /> : null}
                Entrar
              </Button>
            </form>
          </div>
        </section>
      </div>
    </main>
  )
}
