import { useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from '@tanstack/react-router'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import * as zod from 'zod'
import { toast } from 'sonner'
import {
  LogOut,
  Briefcase,
  Package,
  Truck,
  History,
  Edit3,
  Scale,
  CheckSquare,
  FileText,
  Activity,
  Users
} from 'lucide-react'

import { api } from '@/lib/api/client'
import { apiErrorMessage } from '@/lib/api/errors'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { ThemeToggle } from '@/components/theme-toggle'
import { logout } from '@/lib/auth/auth-api'
import { useSessionStore } from '@/lib/auth/session-store'

// Tab components
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

export function AppPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const user = useSessionStore((state) => state.user)
  const activeOrgId = useSessionStore((state) => state.activeOrganizationId)
  const setActiveOrgId = useSessionStore((state) => state.setActiveOrganizationId)
  
  const [activeTab, setActiveTab] = useState<string>('projects')
  const [selectedProjectId, setSelectedProjectId] = useState<string | null>(null)
  const [selectedBudgetId, setSelectedBudgetId] = useState<string | null>(null)
  const [selectedVersionId, setSelectedVersionId] = useState<string | null>(null)

  // Fetch all user organizations
  const orgsQuery = useQuery({
    ...api.queryOptions('get', '/api/organizations'),
    enabled: !!user,
  })

  // Mutations
  const createOrgMutation = api.useMutation('post', '/api/organizations', {
    onSuccess: (res) => {
      toast.success(`Organización "${res.name}" creada con éxito`)
      queryClient.invalidateQueries({ queryKey: ['get', '/api/organizations'] })
      setActiveOrgId(res.id!)
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
            <div className="text-[10px] uppercase font-bold text-muted-foreground tracking-wider px-3 mb-2">
              Gestión Comercial
            </div>
            <button
              onClick={() => setActiveTab('projects')}
              className={`flex w-full items-center gap-3 px-3 py-2 rounded-lg text-xs font-semibold transition-all ${
                activeTab === 'projects' ? 'bg-primary text-primary-foreground shadow-xs' : 'text-muted-foreground hover:bg-accent/40'
              }`}
            >
              <Briefcase className="size-4 shrink-0" />
              Clientes y Proyectos
            </button>
            <button
              onClick={() => setActiveTab('catalog')}
              className={`flex w-full items-center gap-3 px-3 py-2 rounded-lg text-xs font-semibold transition-all ${
                activeTab === 'catalog' ? 'bg-primary text-primary-foreground shadow-xs' : 'text-muted-foreground hover:bg-accent/40'
              }`}
            >
              <Package className="size-4 shrink-0" />
              Catálogo Técnico
            </button>
            <button
              onClick={() => setActiveTab('suppliers')}
              className={`flex w-full items-center gap-3 px-3 py-2 rounded-lg text-xs font-semibold transition-all ${
                activeTab === 'suppliers' ? 'bg-primary text-primary-foreground shadow-xs' : 'text-muted-foreground hover:bg-accent/40'
              }`}
            >
              <Truck className="size-4 shrink-0" />
              Proveedores y Cotización
            </button>

            <div className="text-[10px] uppercase font-bold text-muted-foreground tracking-wider px-3 pt-6 mb-2">
              Presupuestos
            </div>
            <button
              onClick={() => setActiveTab('budgets')}
              className={`flex w-full items-center gap-3 px-3 py-2 rounded-lg text-xs font-semibold transition-all ${
                activeTab === 'budgets' ? 'bg-primary text-primary-foreground shadow-xs' : 'text-muted-foreground hover:bg-accent/40'
              }`}
            >
              <History className="size-4 shrink-0" />
              Versiones y Tasas
            </button>
            <button
              onClick={() => setActiveTab('editor')}
              className={`flex w-full items-center gap-3 px-3 py-2 rounded-lg text-xs font-semibold transition-all ${
                activeTab === 'editor' ? 'bg-primary text-primary-foreground shadow-xs' : 'text-muted-foreground hover:bg-accent/40'
              }`}
            >
              <Edit3 className="size-4 shrink-0" />
              Planilla Editor
            </button>
            <button
              onClick={() => setActiveTab('scenarios')}
              className={`flex w-full items-center gap-3 px-3 py-2 rounded-lg text-xs font-semibold transition-all ${
                activeTab === 'scenarios' ? 'bg-primary text-primary-foreground shadow-xs' : 'text-muted-foreground hover:bg-accent/40'
              }`}
            >
              <Scale className="size-4 shrink-0" />
              Escenarios Comparación
            </button>
            <button
              onClick={() => setActiveTab('approvals')}
              className={`flex w-full items-center gap-3 px-3 py-2 rounded-lg text-xs font-semibold transition-all ${
                activeTab === 'approvals' ? 'bg-primary text-primary-foreground shadow-xs' : 'text-muted-foreground hover:bg-accent/40'
              }`}
            >
              <CheckSquare className="size-4 shrink-0" />
              Flujo Aprobación
            </button>

            <div className="text-[10px] uppercase font-bold text-muted-foreground tracking-wider px-3 pt-6 mb-2">
              Reportes y Seguridad
            </div>
            <button
              onClick={() => setActiveTab('documents')}
              className={`flex w-full items-center gap-3 px-3 py-2 rounded-lg text-xs font-semibold transition-all ${
                activeTab === 'documents' ? 'bg-primary text-primary-foreground shadow-xs' : 'text-muted-foreground hover:bg-accent/40'
              }`}
            >
              <FileText className="size-4 shrink-0" />
              Documentos PDF
            </button>
            <button
              onClick={() => setActiveTab('audit')}
              className={`flex w-full items-center gap-3 px-3 py-2 rounded-lg text-xs font-semibold transition-all ${
                activeTab === 'audit' ? 'bg-primary text-primary-foreground shadow-xs' : 'text-muted-foreground hover:bg-accent/40'
              }`}
            >
              <Activity className="size-4 shrink-0" />
              Auditoría
            </button>
            <button
              onClick={() => setActiveTab('organization')}
              className={`flex w-full items-center gap-3 px-3 py-2 rounded-lg text-xs font-semibold transition-all ${
                activeTab === 'organization' ? 'bg-primary text-primary-foreground shadow-xs' : 'text-muted-foreground hover:bg-accent/40'
              }`}
            >
              <Users className="size-4 shrink-0" />
              Miembros y Usuarios
            </button>
          </nav>
        </aside>

        {/* Dynamic Content Surface */}
        <main className="flex-1 overflow-y-auto bg-background p-6">
          <div className="w-full max-w-6xl mx-auto">
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
          </div>
        </main>
      </div>
    </div>
  )
}
