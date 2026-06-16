import { ArrowRight, Database, FileCode2, LogIn, ShieldCheck } from 'lucide-react'

import { ThemeToggle } from '@/components/theme-toggle'
import { Button } from '@/components/ui/button'

const foundationItems = [
  {
    title: 'Backend seguro',
    text: 'Spring Boot, PostgreSQL, Flyway, Actuator minimo y denegacion por defecto.',
    icon: ShieldCheck,
  },
  {
    title: 'Contrato verificable',
    text: 'OpenAPI nace en el backend y alimenta los tipos del frontend.',
    icon: FileCode2,
  },
  {
    title: 'Base de trabajo',
    text: 'Router, Query Client, Tailwind, shadcn/ui y pruebas listas para modulos reales.',
    icon: Database,
  },
]

export function HomePage() {
  return (
    <main className="min-h-dvh bg-background text-foreground">
      <div className="mx-auto flex min-h-dvh w-full max-w-6xl flex-col px-4 py-4 sm:px-6 lg:px-8">
        <header className="flex h-12 items-center justify-between border-b border-border">
          <a href="/" className="flex items-center gap-3 rounded-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring">
            <span className="flex size-8 items-center justify-center rounded-md bg-primary text-sm font-semibold text-primary-foreground">
              F
            </span>
            <span className="text-sm font-semibold tracking-normal">FORZE</span>
          </a>
          <ThemeToggle />
        </header>

        <section className="grid flex-1 items-center gap-10 py-12 lg:grid-cols-[minmax(0,1fr)_360px]">
          <div className="max-w-3xl">
            <p className="mb-3 text-sm font-medium text-primary">Base tecnica inicializada</p>
            <h1 className="max-w-2xl text-4xl font-semibold leading-tight tracking-normal text-balance sm:text-5xl">
              FORZE prepara el terreno para presupuestos de obra verificables.
            </h1>
            <p className="mt-5 max-w-2xl text-base leading-7 text-muted-foreground text-pretty">
              Monorepo con frontend React/TypeScript y backend Java/Spring Boot listo para construir modulos reales de presupuestacion, sin datos de muestra ni dominio inventado.
            </p>

            <div className="mt-8 flex flex-wrap gap-3">
              <Button asChild>
                <a href="/login">
                  Iniciar sesion
                  <LogIn aria-hidden="true" />
                </a>
              </Button>
              <Button asChild variant="outline">
                <a href="http://localhost:8080/v3/api-docs">
                  Abrir OpenAPI
                  <ArrowRight aria-hidden="true" />
                </a>
              </Button>
            </div>
          </div>

          <aside className="border border-border bg-panel p-4">
            <h2 className="text-sm font-semibold">Cimientos confirmados</h2>
            <div className="mt-4 divide-y divide-border">
              {foundationItems.map((item) => (
                <div className="flex gap-3 py-4 first:pt-0 last:pb-0" key={item.title}>
                  <item.icon className="mt-0.5 size-4 shrink-0 text-primary" aria-hidden="true" />
                  <div>
                    <h3 className="text-sm font-medium">{item.title}</h3>
                    <p className="mt-1 text-sm leading-6 text-muted-foreground">{item.text}</p>
                  </div>
                </div>
              ))}
            </div>
          </aside>
        </section>
      </div>
    </main>
  )
}
