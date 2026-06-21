import { useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from '@tanstack/react-router'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import * as zod from 'zod'
import { toast } from 'sonner'
import {
  LogOut,
  Home,
  Briefcase,
  Package,
  Truck,
  History,
  Edit3,
  Scale,
  CheckSquare,
  FileText,
  Activity,
  Users,
  ShieldAlert,
  Loader2,
  WifiOff,
  RefreshCw,
  type LucideIcon
} from 'lucide-react'

import { api } from '@/lib/api/client'
import { apiErrorMessage } from '@/lib/api/errors'
import { useEffectiveAccess, type Permission } from '@/lib/auth/permissions'
import { resolveAccessView, errorStatus } from '@/app/access-state'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { ThemeToggle } from '@/components/theme-toggle'
import { logout } from '@/lib/auth/auth-api'
import { useSessionStore } from '@/lib/auth/session-store'

// Tab components
import { HomeTab } from './home-tab'
import { CommandPalette, type PaletteAction } from './command-palette'
import { OrganizationTab } from './organization-tab'
import { ClientsProjectsTab } from './clients-projects-tab'
import { CatalogTab } from './catalog-tab'
import { SuppliersTab } from './suppliers-tab'
import { BudgetsTab } from './budgets-tab'
import { EditorTab } from './editor-tab'
import { ScenariosTab } from './scenarios-tab'
import { ApprovalsTab } from './approvals-tab'
import { DocumentsTab } from './documents-tab'
import { AuditTab } from './audit-tab'

const createOrgSchema = zod.object({
  name: zod.string().min(3, 'El nombre debe tener al menos 3 caracteres').max(160),
})
type CreateOrgFormValues = zod.infer<typeof createOrgSchema>

type NavItem = { id: string; label: string; icon: LucideIcon; permission: Permission }
type NavGroup = { group: string; items: NavItem[] }

// Each surface requires a read permission. The menu hides what the user cannot
// read, and the content area shows a 403 if reached without permission. The
// backend @PreAuthorize remains the real authority.
const NAV_GROUPS: NavGroup[] = [
  {
    group: 'Gestión Comercial',
    items: [
      { id: 'projects', label: 'Clientes y Proyectos', icon: Briefcase, permission: 'PROYECTOS_READ' },
      { id: 'catalog', label: 'Catálogo Técnico', icon: Package, permission: 'CATALOGOS_READ' },
      { id: 'suppliers', label: 'Proveedores y Cotización', icon: Truck, permission: 'PROVEEDORES_READ' },
    ],
  },
  {
    group: 'Presupuestos',
    items: [
      { id: 'budgets', label: 'Versiones y Tasas', icon: History, permission: 'PRESUPUESTOS_READ' },
      { id: 'editor', label: 'Planilla Editor', icon: Edit3, permission: 'PRESUPUESTOS_READ' },
      { id: 'scenarios', label: 'Escenarios Comparación', icon: Scale, permission: 'PRESUPUESTOS_READ' },
      { id: 'approvals', label: 'Flujo Aprobación', icon: CheckSquare, permission: 'APROBACIONES_READ' },
    ],
  },
  {
    group: 'Reportes y Seguridad',
    items: [
      { id: 'documents', label: 'Documentos PDF', icon: FileText, permission: 'DOCUMENTOS_READ' },
      { id: 'audit', label: 'Auditoría', icon: Activity, permission: 'AUDITORIA_READ' },
      { id: 'organization', label: 'Miembros y Usuarios', icon: Users, permission: 'ADMINISTRACION_READ' },
    ],
  },
]

const TAB_PERMISSION: Record<string, Permission> = Object.fromEntries(
  NAV_GROUPS.flatMap((g) => g.items).map((i) => [i.id, i.permission]),
) as Record<string, Permission>

function Forbidden403() {
  return (
    <div className="flex flex-col items-center justify-center text-center gap-3 py-20" role="alert">
      <ShieldAlert className="size-10 text-destructive" aria-hidden="true" />
      <h2 className="text-xl font-bold">Acceso denegado (403)</h2>
      <p className="text-sm text-muted-foreground max-w-sm">
        No tienes permisos para ver esta sección en la organización activa. Solicita acceso a un administrador.
      </p>
    </div>
  )
}

function AccessLoading() {
  return (
    <div className="flex flex-col items-center justify-center text-center gap-3 py-20" role="status" aria-live="polite">
      <Loader2 className="size-8 text-primary animate-spin motion-reduce:animate-none" aria-hidden="true" />
      <p className="text-sm text-muted-foreground">Cargando permisos de la organización…</p>
    </div>
  )
}

// Recoverable failure (network/CORS/API): never shown as a 403 access denial.
function AccessError({ onRetry }: { onRetry: () => void }) {
  return (
    <div className="flex flex-col items-center justify-center text-center gap-3 py-20" role="alert">
      <WifiOff className="size-9 text-amber-500" aria-hidden="true" />
      <h2 className="text-lg font-bold">No se pudieron cargar tus permisos</h2>
      <p className="text-sm text-muted-foreground max-w-sm">
        Ocurrió un error de red o de conexión con el servidor. Esto no significa que no tengas acceso.
      </p>
      <Button variant="secondary" size="sm" onClick={onRetry}>
        <RefreshCw className="size-4" />
        Reintentar
      </Button>
    </div>
  )
}

// Stale/invalid active organization: returns to the organization selector instead
// of leaving the UI permanently blocked.
function StaleOrganization({ onReset }: { onReset: () => void }) {
  return (
    <div className="flex flex-col items-center justify-center text-center gap-3 py-20" role="alert">
      <ShieldAlert className="size-9 text-destructive" aria-hidden="true" />
      <h2 className="text-lg font-bold">Organización no disponible</h2>
      <p className="text-sm text-muted-foreground max-w-sm">
        No perteneces a la organización activa o ya no está disponible. Selecciona otra organización para continuar.
      </p>
      <Button variant="secondary" size="sm" onClick={onReset}>
        Volver a seleccionar organización
      </Button>
    </div>
  )
}

export function AppPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const user = useSessionStore((state) => state.user)
  const activeOrgId = useSessionStore((state) => state.activeOrganizationId)
  const setActiveOrgId = useSessionStore((state) => state.setActiveOrganizationId)
  const permissions = useSessionStore((state) => state.permissions)

  // Resolve the current user's role/permissions for the active organization.
  const accessQuery = useEffectiveAccess()

  const [activeTab, setActiveTab] = useState<string>('home')
  const [selectedProjectId, setSelectedProjectId] = useState<string | null>(null)
  const [selectedBudgetId, setSelectedBudgetId] = useState<string | null>(null)
  const [selectedVersionId, setSelectedVersionId] = useState<string | null>(null)
  const [paletteOpen, setPaletteOpen] = useState(false)

  // Fetch all user organizations
  const orgsQuery = useQuery({
    ...api.queryOptions('get', '/api/organizations'),
    enabled: !!user,
  })

  // Active-project selector source. Only queried when the user can read projects.
  const projectsQuery = useQuery({
    ...api.queryOptions('get', '/api/projects'),
    enabled: !!activeOrgId && permissions.includes('PROYECTOS_READ'),
  })

  // Mutations
  const createOrgMutation = api.useMutation('post', '/api/organizations', {
    // Creating a resource must never be retried automatically: one click = one POST.
    retry: false,
    onSuccess: (res) => {
      // Enforce the success contract explicitly: a 2xx without id/name is a
      // contract violation, not a success. Do not activate an organization
      // nor show a success toast for an empty/invalid response.
      if (!res || !res.id || !res.name) {
        toast.error('La organización no se creó correctamente (respuesta inválida del servidor).')
        return
      }
      toast.success(`Organización "${res.name}" creada con éxito`)
      queryClient.invalidateQueries({ queryKey: ['get', '/api/organizations'] })
      setActiveOrgId(res.id)
      orgForm.reset()
    },
    onError: (err) => {
      toast.error(apiErrorMessage(err, 'Error al crear la organización'))
    },
  })

  const orgForm = useForm<CreateOrgFormValues>({
    resolver: zodResolver(createOrgSchema),
    defaultValues: { name: '' },
  })

  async function handleLogout() {
    await logout()
    toast.success('Sesión cerrada')
    await navigate({ to: '/login' })
  }

  function handleCreateOrganization(values: CreateOrgFormValues) {
    createOrgMutation.mutate({ body: { name: values.name } })
  }

  // If no organization is selected, bootstrapper is rendered
  if (!activeOrgId) {
    return (
      <main className="min-h-dvh bg-background text-foreground flex items-center justify-center p-4">
        <div className="w-full max-w-md bg-panel border border-border p-6 rounded-2xl shadow-lg space-y-6 animate-scale-up">
          <div className="text-center space-y-2">
            <h1 className="text-3xl font-extrabold tracking-tight">Bienvenido a FORZE</h1>
            <p className="text-sm text-muted-foreground">
              Para comenzar a estructurar presupuestos, selecciona una organización existente o crea una nueva.
            </p>
            {user && (
              <div className="pt-2">
                <span className="text-xs text-emerald-500 bg-emerald-500/10 px-2 py-0.5 rounded-full font-bold">
                  Refresh automatico activo
                </span>
              </div>
            )}
          </div>

          {/* Select Organization */}
          {orgsQuery.data && orgsQuery.data.length > 0 && (
            <div className="space-y-3 border-b border-border/80 pb-5">
              <label className="text-sm font-semibold text-foreground" htmlFor="orgSelect">
                Organizaciones Disponibles
              </label>
              <select
                id="orgSelect"
                className="flex h-10 w-full rounded-md border border-border bg-background px-3 py-1 text-sm shadow-xs focus-visible:outline-hidden"
              >
                {orgsQuery.data.map((org) => (
                  <option key={org.id} value={org.id}>
                    {org.name}
                  </option>
                ))}
              </select>
              <Button
                className="w-full"
                onClick={() => {
                  const select = document.getElementById('orgSelect') as HTMLSelectElement
                  if (select.value) {
                    setActiveOrgId(select.value)
                    queryClient.invalidateQueries()
                    toast.success('Organización activa seleccionada')
                  }
                }}
              >
                Ingresar a Organización
              </Button>
            </div>
          )}

          {/* Create Organization Form */}
          <form onSubmit={orgForm.handleSubmit(handleCreateOrganization)} className="space-y-4">
            <div className="space-y-2">
              <label className="text-sm font-semibold text-foreground">Crear Nueva Organización</label>
              <Input
                placeholder="Nombre comercial de la empresa"
                {...orgForm.register('name')}
                aria-label="Nombre comercial de la empresa"
              />
              {orgForm.formState.errors.name && (
                <p className="text-xs text-destructive">{orgForm.formState.errors.name.message}</p>
              )}
            </div>
            <Button type="submit" variant="secondary" className="w-full" disabled={createOrgMutation.isPending}>
              Crear e Ingresar
            </Button>
          </form>

          {/* Logout button */}
          <div className="flex justify-between items-center border-t border-border pt-4">
            <ThemeToggle />
            <Button variant="ghost" onClick={handleLogout} className="text-xs" aria-label="salir">
              <LogOut className="size-4" />
              Cerrar Sesión
            </Button>
          </div>
        </div>
      </main>
    )
  }

  // Distinguish access states so a pending/failed access query is never rendered
  // as a permission denial. The backend remains the authority.
  const requiredPermission = TAB_PERMISSION[activeTab]
  const accessView = resolveAccessView({
    isPending: accessQuery.isPending,
    isFetching: accessQuery.isFetching,
    isError: accessQuery.isError,
    errorStatus: errorStatus(accessQuery.error),
    permissionsCount: permissions.length,
    hasRequiredPermission: !requiredPermission || permissions.includes(requiredPermission),
  })

  function resetOrganization() {
    setActiveOrgId(null)
    queryClient.removeQueries()
  }

  // Command palette: navigation to readable surfaces + permitted quick-creates.
  // Permission filtering happens inside the palette; the backend stays authority.
  const paletteActions: PaletteAction[] = [
    { id: 'nav-home', label: 'Ir a Inicio', group: 'Navegación', run: () => setActiveTab('home') },
    ...NAV_GROUPS.flatMap((g) => g.items).map((item) => ({
      id: `nav-${item.id}`,
      label: `Ir a ${item.label}`,
      group: 'Navegación',
      permission: item.permission,
      run: () => setActiveTab(item.id),
    })),
    { id: 'create-project', label: 'Crear proyecto', group: 'Crear', permission: 'PROYECTOS_WRITE' as Permission, run: () => setActiveTab('projects') },
    { id: 'create-budget', label: 'Nuevo presupuesto', group: 'Crear', permission: 'PRESUPUESTOS_WRITE' as Permission, run: () => setActiveTab('budgets') },
    { id: 'create-supplier', label: 'Agregar proveedor', group: 'Crear', permission: 'PROVEEDORES_WRITE' as Permission, run: () => setActiveTab('suppliers') },
    { id: 'create-insumo', label: 'Nuevo insumo', group: 'Crear', permission: 'CATALOGOS_WRITE' as Permission, run: () => setActiveTab('catalog') },
  ]

  return (
    <div className="min-h-dvh bg-background text-foreground flex flex-col font-sans">
      {/* Top Header */}
      <header className="h-14 border-b border-border bg-panel flex items-center justify-between px-6 shrink-0 z-40">
        <div className="flex items-center gap-6">
          <a href="/app" className="font-extrabold text-lg tracking-wider text-primary">
            FORZE
          </a>

          {/* Org Selector Switcher */}
          <div className="flex items-center gap-1">
            <span className="text-xs text-muted-foreground font-semibold">Empresa:</span>
            <select
              className="h-8 rounded bg-background border border-border px-2 text-xs font-semibold focus-visible:outline-hidden"
              value={activeOrgId}
              onChange={(e) => {
                setActiveOrgId(e.target.value)
                setSelectedProjectId(null)
                setSelectedBudgetId(null)
                setSelectedVersionId(null)
                queryClient.invalidateQueries()
                toast.success('Organización cambiada')
              }}
              aria-label="Seleccionar organización activa"
            >
              {orgsQuery.data?.map((org) => (
                <option key={org.id} value={org.id}>
                  {org.name}
                </option>
              ))}
            </select>
          </div>

          {/* Active project selector — shared across budget/editor/scenario surfaces. */}
          {permissions.includes('PROYECTOS_READ') && (projectsQuery.data?.length ?? 0) > 0 && (
            <div className="hidden items-center gap-1 md:flex">
              <span className="text-xs text-muted-foreground font-semibold">Proyecto:</span>
              <select
                className="h-8 rounded bg-background border border-border px-2 text-xs font-semibold focus-visible:outline-hidden"
                value={selectedProjectId ?? ''}
                onChange={(e) => {
                  setSelectedProjectId(e.target.value || null)
                  setSelectedBudgetId(null)
                  setSelectedVersionId(null)
                }}
                aria-label="Seleccionar proyecto activo"
              >
                <option value="">Sin proyecto</option>
                {projectsQuery.data?.map((project) => (
                  <option key={project.id} value={project.id}>
                    {project.name}
                  </option>
                ))}
              </select>
            </div>
          )}
        </div>

        <div className="flex items-center gap-4">
          <span className="hidden sm:inline text-xs text-muted-foreground font-medium">
            Usuario: <strong className="text-foreground">{user?.username}</strong>
          </span>
          <span className="text-xs text-emerald-500 bg-emerald-500/10 px-2 py-0.5 rounded-full font-bold">
            Refresh automatico activo
          </span>
          <ThemeToggle />
          <Button variant="ghost" size="icon" onClick={handleLogout} title="Cerrar Sesión" aria-label="salir">
            <LogOut className="size-4 text-muted-foreground hover:text-foreground" />
          </Button>
        </div>
      </header>

      {/* Main Layout Body */}
      <div className="flex-1 flex overflow-hidden">
        {/* Sidebar Navigation */}
        <aside className="w-60 border-r border-border bg-panel flex flex-col p-4 shrink-0 overflow-y-auto">
          <nav className="space-y-1" aria-label="Navegación principal">
            {/* Inicio is available to any member of the active organization. */}
            <button
              onClick={() => setActiveTab('home')}
              aria-current={activeTab === 'home' ? 'page' : undefined}
              className={`flex w-full items-center gap-3 px-3 py-2 rounded-lg text-xs font-semibold transition-all ${
                activeTab === 'home' ? 'bg-primary text-primary-foreground shadow-xs' : 'text-muted-foreground hover:bg-accent/40'
              }`}
            >
              <Home className="size-4 shrink-0" />
              Inicio
            </button>
            {NAV_GROUPS.map((group) => {
              const visible = group.items.filter((item) => permissions.includes(item.permission))
              if (visible.length === 0) return null
              return (
                <div key={group.group}>
                  <div className="text-[10px] uppercase font-bold text-muted-foreground tracking-wider px-3 pt-6 first:pt-0 mb-2">
                    {group.group}
                  </div>
                  {visible.map((item) => {
                    const Icon = item.icon
                    return (
                      <button
                        key={item.id}
                        onClick={() => setActiveTab(item.id)}
                        aria-current={activeTab === item.id ? 'page' : undefined}
                        className={`flex w-full items-center gap-3 px-3 py-2 rounded-lg text-xs font-semibold transition-all ${
                          activeTab === item.id ? 'bg-primary text-primary-foreground shadow-xs' : 'text-muted-foreground hover:bg-accent/40'
                        }`}
                      >
                        <Icon className="size-4 shrink-0" />
                        {item.label}
                      </button>
                    )
                  })}
                </div>
              )
            })}
          </nav>
        </aside>

        {/* Dynamic Content Surface */}
        <main className="flex-1 overflow-y-auto bg-background p-6">
          <div className="w-full max-w-6xl mx-auto">
            {accessView === 'loading' ? (
              <AccessLoading />
            ) : accessView === 'stale-organization' ? (
              <StaleOrganization onReset={resetOrganization} />
            ) : accessView === 'network-error' ? (
              <AccessError onRetry={() => accessQuery.refetch()} />
            ) : accessView === 'forbidden' ? (
              <Forbidden403 />
            ) : (
              <>
            {activeTab === 'home' && (
              <HomeTab permissions={permissions} onNavigate={setActiveTab} onOpenPalette={() => setPaletteOpen(true)} />
            )}
            {activeTab === 'organization' && <OrganizationTab />}
            {activeTab === 'projects' && <ClientsProjectsTab />}
            {activeTab === 'catalog' && <CatalogTab />}
            {activeTab === 'suppliers' && <SuppliersTab />}
            {activeTab === 'budgets' && (
              <BudgetsTab
                selectedProjectId={selectedProjectId}
                setSelectedProjectId={setSelectedProjectId}
                selectedBudgetId={selectedBudgetId}
                setSelectedBudgetId={setSelectedBudgetId}
                selectedVersionId={selectedVersionId}
                setSelectedVersionId={setSelectedVersionId}
                setActiveTab={setActiveTab}
              />
            )}
            {activeTab === 'editor' && <EditorTab selectedVersionId={selectedVersionId} />}
            {activeTab === 'scenarios' && <ScenariosTab selectedVersionId={selectedVersionId} />}
            {activeTab === 'approvals' && <ApprovalsTab selectedVersionId={selectedVersionId} />}
            {activeTab === 'documents' && <DocumentsTab selectedVersionId={selectedVersionId} />}
            {activeTab === 'audit' && <AuditTab />}
              </>
            )}
          </div>
        </main>
      </div>

      <CommandPalette
        open={paletteOpen}
        onOpenChange={setPaletteOpen}
        permissions={permissions}
        actions={paletteActions}
      />
    </div>
  )
}
