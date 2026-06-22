import { Sparkles, ArrowRight, KeyRound } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { DEMO_ACCOUNTS, DEMO_PASSWORD } from '@/lib/env'

type DemoGuideProps = {
  /** True once the demo dataset (DEMO-* records) is detected. */
  demoLoaded: boolean
  loading: boolean
  onNavigate: (tab: string) => void
}

const TOUR: { label: string; hint: string; tab: string }[] = [
  { label: 'Proyectos demo', hint: 'Edificio Residencial / Bodega Industrial', tab: 'projects' },
  { label: 'Presupuestos', hint: 'PPTO-001 viable · PPTO-002 con alertas', tab: 'budgets' },
  { label: 'Editor (APU y mediciones)', hint: 'Rubros con APU y cómputo métrico', tab: 'editor' },
  { label: 'Proveedores y precios', hint: 'Cotización demo e historial de precios', tab: 'suppliers' },
  { label: 'Escenarios', hint: 'Comparar escenario económico vs base', tab: 'scenarios' },
  { label: 'Aprobaciones', hint: 'PPTO-001 enviado y aprobado', tab: 'approvals' },
  { label: 'Documentos', hint: 'Cotización PDF generada', tab: 'documents' },
  { label: 'Auditoría', hint: 'Eventos de aprobación y administración', tab: 'audit' },
]

/**
 * Dev/local-only guide to the seeded demo dataset. The caller renders it only
 * when running under the Vite dev server, so it never ships to production.
 */
export function DemoGuide({ demoLoaded, loading, onNavigate }: DemoGuideProps) {
  return (
    <section
      className="rounded-xl border border-primary/30 bg-primary/5 p-5"
      aria-label="Guía de datos demo"
    >
      <div className="mb-3 flex items-center gap-2">
        <Sparkles className="size-5 text-primary" aria-hidden="true" />
        <h2 className="text-sm font-semibold">Modo demo (solo local/dev)</h2>
        <span className="ml-auto rounded-full border border-primary/40 px-2 py-0.5 text-[10px] font-bold uppercase tracking-wider text-primary">
          dev
        </span>
      </div>

      {loading ? (
        <div className="h-7 w-3/4 animate-pulse rounded bg-muted" role="status" aria-label="Cargando" />
      ) : !demoLoaded ? (
        <div className="space-y-2 text-sm">
          <p className="text-muted-foreground">
            Los datos demo no están cargados. Arranca el backend en perfil <code className="font-mono">dev</code> —
            el seeder los crea automáticamente (idempotente). Luego recarga esta página.
          </p>
          <Button variant="secondary" size="sm" onClick={() => onNavigate('projects')}>
            Ir a Proyectos
            <ArrowRight className="size-4" />
          </Button>
        </div>
      ) : (
        <div className="space-y-4">
          {/* Credentials */}
          <div className="rounded-lg border border-border bg-background p-3">
            <div className="mb-2 flex items-center gap-1.5 text-xs font-semibold text-muted-foreground">
              <KeyRound className="size-3.5" aria-hidden="true" />
              Credenciales locales ficticias — contraseña{' '}
              <code className="font-mono text-foreground">{DEMO_PASSWORD}</code>
            </div>
            <ul className="grid gap-1 sm:grid-cols-2">
              {DEMO_ACCOUNTS.map((acc) => (
                <li key={acc.username} className="flex items-baseline justify-between gap-2 text-xs">
                  <span className="text-muted-foreground">{acc.role}</span>
                  <code className="truncate font-mono text-foreground">{acc.username}</code>
                </li>
              ))}
            </ul>
          </div>

          {/* Guided tour shortcuts */}
          <div className="grid gap-2 sm:grid-cols-2">
            {TOUR.map((step) => (
              <button
                key={step.tab}
                onClick={() => onNavigate(step.tab)}
                className="flex items-center justify-between gap-2 rounded-md border border-border bg-background px-3 py-2 text-left text-sm transition-colors hover:bg-accent/40 focus-visible:outline-hidden focus-visible:ring-2 focus-visible:ring-ring"
              >
                <span>
                  <span className="block font-medium">{step.label}</span>
                  <span className="block text-[11px] text-muted-foreground">{step.hint}</span>
                </span>
                <ArrowRight className="size-4 shrink-0 text-muted-foreground" aria-hidden="true" />
              </button>
            ))}
          </div>
        </div>
      )}
    </section>
  )
}
