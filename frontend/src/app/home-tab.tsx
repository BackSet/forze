import { useQuery } from '@tanstack/react-query'
import {
  Briefcase,
  Package,
  Truck,
  Users,
  Activity,
  ShieldAlert,
  Plus,
  ArrowRight,
  CheckCircle2,
  type LucideIcon,
} from 'lucide-react'

import { api } from '@/lib/api/client'
import { Button } from '@/components/ui/button'
import { useSessionStore } from '@/lib/auth/session-store'
import type { Permission } from '@/lib/auth/permissions'
import { env } from '@/lib/env'
import { DemoGuide } from './demo-guide'

type HomeTabProps = {
  permissions: string[]
  onNavigate: (tab: string) => void
  onOpenPalette: () => void
}

export function HomeTab({ permissions, onNavigate, onOpenPalette }: HomeTabProps) {
  const user = useSessionStore((s) => s.user)
  const can = (p: Permission) => permissions.includes(p)

  const projectsQuery = useQuery({
    ...api.queryOptions('get', '/api/projects'),
    enabled: can('PROYECTOS_READ'),
  })
  const suppliersQuery = useQuery({
    ...api.queryOptions('get', '/api/suppliers'),
    enabled: can('PROVEEDORES_READ'),
  })
  const insumosQuery = useQuery({
    ...api.queryOptions('get', '/api/insumos'),
    enabled: can('CATALOGOS_READ'),
  })
  const membersQuery = useQuery({
    ...api.queryOptions('get', '/api/members'),
    enabled: can('ADMINISTRACION_READ'),
  })
  const auditQuery = useQuery({
    ...api.queryOptions('get', '/api/audit-logs'),
    enabled: can('AUDITORIA_READ'),
  })

  const projects = projectsQuery.data ?? []
  // Demo dataset detection (DEMO-* codes) for the dev-only guide.
  const demoLoaded = projects.some((p) => p.code?.startsWith('DEMO-'))
  const activeProjects = projects.filter((p) => p.status === 'ACTIVO')
  // Real, derived task signal: active projects that do not have a current budget yet.
  const projectsWithoutBudget = activeProjects.filter((p) => !p.currentBudgetId)
  const recentActivity = (auditQuery.data ?? []).slice(0, 6)

  const allQuickActions: { label: string; permission: Permission; tab: string }[] = [
    { label: 'Crear proyecto', permission: 'PROYECTOS_WRITE', tab: 'projects' },
    { label: 'Nuevo presupuesto', permission: 'PRESUPUESTOS_WRITE', tab: 'budgets' },
    { label: 'Agregar proveedor', permission: 'PROVEEDORES_WRITE', tab: 'suppliers' },
    { label: 'Nuevo insumo', permission: 'CATALOGOS_WRITE', tab: 'catalog' },
  ]
  const quickActions = allQuickActions.filter((a) => can(a.permission))

  return (
    <div className="space-y-8 animate-fade-in">
      <div className="flex flex-col gap-2 border-b border-border pb-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">
            Hola{user?.username ? `, ${user.username}` : ''}
          </h1>
          <p className="text-sm text-muted-foreground">Resumen operativo de la organización activa.</p>
        </div>
        <Button variant="secondary" size="sm" onClick={onOpenPalette} aria-label="Abrir paleta de comandos">
          Buscar acciones
          <kbd className="ml-1 rounded border border-border bg-background px-1.5 py-0.5 text-[10px] font-semibold">⌘K</kbd>
        </Button>
      </div>

      {env.isDev && can('PROYECTOS_READ') && (
        <DemoGuide demoLoaded={demoLoaded} loading={projectsQuery.isPending} onNavigate={onNavigate} />
      )}

      {/* KPI cards — each only appears when the user can read its source. */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {can('PROYECTOS_READ') && (
          <KpiCard
            icon={Briefcase}
            label="Proyectos activos"
            value={projectsQuery.isPending ? null : activeProjects.length}
            hint={`${projects.length} en total`}
            onClick={() => onNavigate('projects')}
          />
        )}
        {can('CATALOGOS_READ') && (
          <KpiCard
            icon={Package}
            label="Insumos en catálogo"
            value={insumosQuery.isPending ? null : (insumosQuery.data ?? []).length}
            onClick={() => onNavigate('catalog')}
          />
        )}
        {can('PROVEEDORES_READ') && (
          <KpiCard
            icon={Truck}
            label="Proveedores"
            value={suppliersQuery.isPending ? null : (suppliersQuery.data ?? []).length}
            onClick={() => onNavigate('suppliers')}
          />
        )}
        {can('ADMINISTRACION_READ') && (
          <KpiCard
            icon={Users}
            label="Miembros"
            value={membersQuery.isPending ? null : (membersQuery.data ?? []).length}
            onClick={() => onNavigate('organization')}
          />
        )}
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        {/* Pending tasks */}
        {can('PROYECTOS_READ') && (
          <Panel title="Tareas pendientes" icon={CheckCircle2}>
            {projectsQuery.isPending ? (
              <SkeletonRows />
            ) : projectsWithoutBudget.length === 0 ? (
              <EmptyState message="No hay proyectos activos sin presupuesto. Todo al día." />
            ) : (
              <ul className="space-y-1">
                {projectsWithoutBudget.slice(0, 6).map((p) => (
                  <li key={p.id}>
                    <button
                      onClick={() => onNavigate('budgets')}
                      className="flex w-full items-center justify-between rounded-md px-2 py-2 text-left text-sm hover:bg-accent/40"
                    >
                      <span className="truncate">{p.name}</span>
                      <span className="ml-2 shrink-0 text-xs text-amber-500">Sin presupuesto</span>
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </Panel>
        )}

        {/* Recent activity */}
        {can('AUDITORIA_READ') && (
          <Panel title="Actividad reciente" icon={Activity}>
            {auditQuery.isPending ? (
              <SkeletonRows />
            ) : recentActivity.length === 0 ? (
              <EmptyState message="Sin actividad registrada todavía." />
            ) : (
              <ul className="space-y-1">
                {recentActivity.map((log) => (
                  <li key={log.id} className="flex items-center justify-between gap-2 rounded-md px-2 py-1.5 text-sm">
                    <span className="truncate">
                      <span className="font-medium">{log.action}</span>{' '}
                      <span className="text-muted-foreground">{log.entityType}</span>
                    </span>
                    <span className="shrink-0 text-xs text-muted-foreground">
                      {log.occurredAt ? new Date(log.occurredAt).toLocaleDateString() : ''}
                    </span>
                  </li>
                ))}
              </ul>
            )}
          </Panel>
        )}

        {/* Critical alerts: evaluated per budget version, so this is an actionable
            pointer rather than a decorative metric without a source. */}
        <Panel title="Alertas críticas" icon={ShieldAlert}>
          <EmptyState message="Las alertas de calidad y viabilidad se evalúan por versión de presupuesto." />
          {can('PRESUPUESTOS_READ') && (
            <Button variant="ghost" size="sm" className="mt-2" onClick={() => onNavigate('budgets')}>
              Revisar presupuestos
              <ArrowRight className="size-4" />
            </Button>
          )}
        </Panel>
      </div>

      {/* Quick create */}
      {quickActions.length > 0 && (
        <Panel title="Creación rápida" icon={Plus}>
          <div className="flex flex-wrap gap-2">
            {quickActions.map((action) => (
              <Button key={action.tab} variant="secondary" size="sm" onClick={() => onNavigate(action.tab)}>
                <Plus className="size-4" />
                {action.label}
              </Button>
            ))}
          </div>
        </Panel>
      )}
    </div>
  )
}

function KpiCard({
  icon: Icon,
  label,
  value,
  hint,
  onClick,
}: {
  icon: LucideIcon
  label: string
  value: number | null
  hint?: string
  onClick: () => void
}) {
  return (
    <button
      onClick={onClick}
      className="flex flex-col items-start gap-2 rounded-xl border border-border bg-panel p-4 text-left transition-colors hover:bg-accent/40 focus-visible:outline-hidden focus-visible:ring-2 focus-visible:ring-ring"
    >
      <Icon className="size-5 text-primary" aria-hidden="true" />
      <span className="text-2xl font-bold tabular-nums">
        {value === null ? <span className="inline-block h-7 w-10 animate-pulse rounded bg-muted align-middle" /> : value}
      </span>
      <span className="text-xs font-medium text-muted-foreground">{label}</span>
      {hint && <span className="text-[11px] text-muted-foreground/80">{hint}</span>}
    </button>
  )
}

function Panel({ title, icon: Icon, children }: { title: string; icon: LucideIcon; children: React.ReactNode }) {
  return (
    <section className="rounded-xl border border-border bg-panel p-5">
      <div className="mb-3 flex items-center gap-2 border-b border-border pb-3">
        <Icon className="size-4 text-primary" aria-hidden="true" />
        <h2 className="text-sm font-semibold">{title}</h2>
      </div>
      {children}
    </section>
  )
}

function EmptyState({ message }: { message: string }) {
  return <p className="py-4 text-sm text-muted-foreground">{message}</p>
}

function SkeletonRows() {
  return (
    <div className="space-y-2 py-2" role="status" aria-label="Cargando">
      <div className="h-7 w-full animate-pulse rounded bg-muted" />
      <div className="h-7 w-3/4 animate-pulse rounded bg-muted" />
    </div>
  )
}
