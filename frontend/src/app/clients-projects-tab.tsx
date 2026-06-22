import { useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm, type Resolver } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import * as zod from 'zod'
import { toast } from 'sonner'
import { Briefcase, Building, Plus, Calendar, ChevronRight, Archive, Users, Trash2 } from 'lucide-react'

import { api } from '@/lib/api/client'
import { apiErrorMessage } from '@/lib/api/errors'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { PageHeader } from '@/components/ui/page-header'
import { QuickActionsBar } from '@/components/ui/quick-actions-bar'
import { DataToolbar } from '@/components/ui/data-toolbar'
import { EmptyState } from '@/components/ui/empty-state'
import { StatusBadge } from '@/components/ui/status-badge'
import { FormSection } from '@/components/ui/form-section'
import { Drawer } from '@/components/ui/drawer'
import { CodeField } from '@/components/ui/code-field'
import { ConfirmAction } from '@/components/ui/confirm-action'
import { useSessionStore } from '@/lib/auth/session-store'

const clientSchema = zod.object({
  name: zod.string().min(3, 'Mínimo 3 caracteres').max(200),
})

type ClientFormValues = zod.infer<typeof clientSchema>

const projectSchema = zod.object({
  code: zod.string().min(2, 'Mínimo 2 caracteres').max(60),
  name: zod.string().min(3, 'Mínimo 3 caracteres').max(200),
  clientId: zod.string().uuid('Seleccione un cliente'),
  description: zod.string().optional(),
  workType: zod.string().optional(),
  location: zod.string().optional(),
  estimatedStartDate: zod.string().optional(),
  estimatedEndDate: zod.string().optional(),
  currencyCode: zod.string().length(3).default('USD'),
  targetAmount: zod.coerce.number().min(0, 'Debe ser mayor o igual a 0'),
  minimumMargin: zod.coerce.number().min(0).max(100, 'Debe estar entre 0 y 100').default(10),
  responsibleUserId: zod.string().uuid('Seleccione un responsable').optional().or(zod.string().length(0)),
})

type ProjectFormValues = zod.infer<typeof projectSchema>

const fieldLabel = 'text-sm font-medium text-muted-foreground mb-1 block'
const selectClass =
  'flex h-9 w-full rounded-md border border-border bg-background px-3 py-1 text-sm shadow-xs focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring'

export function ClientsProjectsTab() {
  const queryClient = useQueryClient()
  const activeOrgId = useSessionStore((state) => state.activeOrganizationId)
  const [selectedProjectId, setSelectedProjectId] = useState<string | null>(null)
  const [showClientDrawer, setShowClientDrawer] = useState(false)
  const [showProjectDrawer, setShowProjectDrawer] = useState(false)
  const [searchQuery, setSearchQuery] = useState('')
  const [teamMemberToAdd, setTeamMemberToAdd] = useState('')

  const clientsQuery = useQuery({ ...api.queryOptions('get', '/api/clients'), enabled: !!activeOrgId })
  const projectsQuery = useQuery({ ...api.queryOptions('get', '/api/projects'), enabled: !!activeOrgId })
  const systemUsersQuery = useQuery({ ...api.queryOptions('get', '/api/admin/users'), enabled: !!activeOrgId })
  const projectDetailsQuery = useQuery({
    ...api.queryOptions('get', '/api/projects/{id}', { params: { path: { id: selectedProjectId || '' } } }),
    enabled: !!selectedProjectId,
  })
  const projectTeamQuery = useQuery({
    ...api.queryOptions('get', '/api/projects/{id}/team', { params: { path: { id: selectedProjectId || '' } } }),
    enabled: !!selectedProjectId,
  })

  const createClientMutation = api.useMutation('post', '/api/clients', {
    onSuccess: (created) => {
      toast.success('Cliente creado con éxito')
      queryClient.invalidateQueries({ queryKey: ['get', '/api/clients'] })
      // Quick-create from the project form: preselect the new client.
      if (showProjectDrawer && created?.id) {
        projectForm.setValue('clientId', created.id, { shouldValidate: true })
      }
      setShowClientDrawer(false)
      clientForm.reset()
    },
    onError: (err) => toast.error(apiErrorMessage(err, 'Error al crear cliente')),
  })

  const createProjectMutation = api.useMutation('post', '/api/projects', {
    onSuccess: () => {
      toast.success('Proyecto creado con éxito')
      queryClient.invalidateQueries({ queryKey: ['get', '/api/projects'] })
      setShowProjectDrawer(false)
      projectForm.reset()
    },
    onError: (err) => {
      const message = apiErrorMessage(err, 'Error al crear proyecto')
      toast.error(message)
      // Make a duplicate-code error visible right on the field.
      if (/c[oó]digo/i.test(message)) {
        projectForm.setError('code', { message })
      }
    },
  })

  const archiveProjectMutation = api.useMutation('put', '/api/projects/{id}/archive', {
    onSuccess: () => {
      toast.success('Proyecto archivado')
      queryClient.invalidateQueries({ queryKey: ['get', '/api/projects'] })
      if (selectedProjectId) {
        queryClient.invalidateQueries({ queryKey: ['get', '/api/projects/{id}', { params: { path: { id: selectedProjectId } } }] })
      }
    },
    onError: (err) => toast.error(apiErrorMessage(err, 'Error al archivar proyecto')),
  })

  const addTeamMemberMutation = api.useMutation('post', '/api/projects/{id}/team', {
    onSuccess: () => {
      toast.success('Miembro de equipo agregado')
      setTeamMemberToAdd('')
      queryClient.invalidateQueries({ queryKey: ['get', '/api/projects/{id}/team', { params: { path: { id: selectedProjectId! } } }] })
    },
    onError: (err) => toast.error(apiErrorMessage(err, 'Error al agregar miembro')),
  })

  const removeTeamMemberMutation = api.useMutation('delete', '/api/projects/{id}/team/{userId}', {
    onSuccess: () => {
      toast.success('Miembro de equipo removido')
      queryClient.invalidateQueries({ queryKey: ['get', '/api/projects/{id}/team', { params: { path: { id: selectedProjectId! } } }] })
    },
    onError: (err) => toast.error(apiErrorMessage(err, 'Error al remover miembro')),
  })

  const clientForm = useForm({
    resolver: zodResolver(clientSchema) as unknown as Resolver<ClientFormValues>,
    defaultValues: { name: '' },
  })

  const projectForm = useForm({
    resolver: zodResolver(projectSchema) as unknown as Resolver<ProjectFormValues>,
    defaultValues: {
      code: '', name: '', clientId: '', description: '', workType: '', location: '',
      estimatedStartDate: '', estimatedEndDate: '', currencyCode: 'USD', targetAmount: 0,
      minimumMargin: 10, responsibleUserId: '',
    },
  })

  // Code generation through the typed API layer (no direct HTTP). Confirms before
  // overwriting a code the user already typed manually.
  async function generateProjectCode(): Promise<string> {
    const current = projectForm.getValues('code')
    if (current.trim() && !window.confirm('Ya ingresaste un código. ¿Reemplazarlo por el código generado?')) {
      return current
    }
    const result = await queryClient.fetchQuery(api.queryOptions('get', '/api/projects/next-code'))
    return result.code ?? current
  }

  function handleCreateClient(values: ClientFormValues) {
    createClientMutation.mutate({ body: { name: values.name } })
  }

  function handleCreateProject(values: ProjectFormValues) {
    createProjectMutation.mutate({
      body: {
        code: values.code,
        name: values.name,
        currencyCode: values.currencyCode,
        targetAmount: values.targetAmount,
        minimumMargin: values.minimumMargin / 100,
        ...(values.clientId ? { clientId: values.clientId } : {}),
        ...(values.description ? { description: values.description } : {}),
        ...(values.workType ? { workType: values.workType } : {}),
        ...(values.location ? { location: values.location } : {}),
        ...(values.estimatedStartDate ? { estimatedStartDate: values.estimatedStartDate } : {}),
        ...(values.estimatedEndDate ? { estimatedEndDate: values.estimatedEndDate } : {}),
        ...(values.responsibleUserId ? { responsibleUserId: values.responsibleUserId } : {}),
      },
    })
  }

  const filteredProjects = projectsQuery.data?.filter(
    (p) =>
      p.name?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      p.code?.toLowerCase().includes(searchQuery.toLowerCase()),
  )
  const selectedProject = projectDetailsQuery.data
  const codeError = projectForm.formState.errors.code?.message

  return (
    <div className="space-y-6 animate-fade-in">
      <PageHeader
        title="Clientes y Proyectos"
        description="Administra los clientes comerciales y crea presupuestos por proyecto."
        actions={
          <QuickActionsBar>
            <Button variant="outline" onClick={() => setShowClientDrawer(true)}>
              <Building className="size-4" />
              Nuevo Cliente
            </Button>
            <Button onClick={() => setShowProjectDrawer(true)}>
              <Plus className="size-4" />
              Nuevo Proyecto
            </Button>
          </QuickActionsBar>
        }
      />

      <div className="grid gap-6 lg:grid-cols-[1fr_360px]">
        {/* Projects list */}
        <div className="space-y-4">
          <DataToolbar
            search={searchQuery}
            onSearchChange={setSearchQuery}
            searchPlaceholder="Buscar por código o nombre…"
          />

          {projectsQuery.isLoading ? (
            <div className="space-y-3">
              <div className="h-20 w-full animate-pulse rounded-xl bg-muted" />
              <div className="h-20 w-full animate-pulse rounded-xl bg-muted" />
            </div>
          ) : !filteredProjects || filteredProjects.length === 0 ? (
            <EmptyState
              icon={Briefcase}
              title="No hay proyectos"
              message="Crea un proyecto nuevo para comenzar a presupuestar."
              action={
                <Button onClick={() => setShowProjectDrawer(true)}>
                  <Plus className="size-4" />
                  Nuevo Proyecto
                </Button>
              }
            />
          ) : (
            <div className="grid gap-3">
              {filteredProjects.map((project) => (
                <button
                  key={project.id}
                  onClick={() => setSelectedProjectId(project.id!)}
                  aria-current={selectedProjectId === project.id ? 'true' : undefined}
                  className={`group flex w-full flex-col items-start gap-3 rounded-xl border p-4 text-left transition-colors motion-reduce:transition-none hover:bg-accent/40 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring sm:flex-row sm:items-center sm:justify-between ${
                    selectedProjectId === project.id ? 'border-primary bg-primary/5 shadow-xs' : 'border-border bg-panel'
                  }`}
                >
                  <div className="min-w-0 space-y-1">
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="rounded bg-secondary px-2 py-0.5 font-mono text-xs font-bold text-secondary-foreground">
                        {project.code}
                      </span>
                      <h3 className="truncate text-base font-semibold leading-none group-hover:text-primary">{project.name}</h3>
                      {project.status === 'ARCHIVADO' ? <StatusBadge tone="neutral">Archivado</StatusBadge> : null}
                    </div>
                    <p className="text-xs text-muted-foreground">
                      Cliente: {clientsQuery.data?.find((c) => c.id === project.clientId)?.name || '—'}
                    </p>
                    {project.location ? <p className="text-xs text-muted-foreground">Ubicación: {project.location}</p> : null}
                  </div>
                  <div className="flex w-full items-center justify-between gap-4 border-t border-border pt-2 sm:w-auto sm:border-t-0 sm:pt-0">
                    <div className="text-right">
                      <p className="text-xs text-muted-foreground">Monto objetivo</p>
                      <p className="text-sm font-bold tabular-nums">
                        {project.currencyCode} {project.targetAmount?.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                      </p>
                    </div>
                    <ChevronRight className="size-5 shrink-0 text-muted-foreground transition-transform group-hover:translate-x-1 motion-reduce:transform-none" />
                  </div>
                </button>
              ))}
            </div>
          )}
        </div>

        {/* Detail panel */}
        <div className="space-y-6">
          {selectedProjectId && selectedProject ? (
            <div className="space-y-6 rounded-xl border border-border bg-panel p-5 animate-fade-in">
              <div className="flex items-start justify-between gap-3 border-b border-border pb-3">
                <div className="min-w-0">
                  <div className="font-mono text-xs font-semibold text-muted-foreground">{selectedProject.code}</div>
                  <h2 className="text-lg font-bold">{selectedProject.name}</h2>
                </div>
                {selectedProject.status !== 'ARCHIVADO' ? (
                  <ConfirmAction
                    triggerLabel="Archivar proyecto"
                    message="¿Archivar este proyecto? No aparecerá como activo."
                    confirmLabel="Archivar"
                    destructive
                    disabled={archiveProjectMutation.isPending}
                    onConfirm={() => archiveProjectMutation.mutate({ params: { path: { id: selectedProject.id! } } })}
                  >
                    <Archive className="size-4" />
                  </ConfirmAction>
                ) : null}
              </div>

              <div className="space-y-4 text-sm">
                {selectedProject.description ? (
                  <div>
                    <h4 className="text-xs font-semibold text-muted-foreground">Descripción</h4>
                    <p className="mt-1">{selectedProject.description}</p>
                  </div>
                ) : null}
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <h4 className="text-xs font-semibold text-muted-foreground">Tipo de obra</h4>
                    <p className="mt-1 font-medium">{selectedProject.workType || '—'}</p>
                  </div>
                  <div>
                    <h4 className="text-xs font-semibold text-muted-foreground">Estado</h4>
                    <div className="mt-1">
                      <StatusBadge tone={selectedProject.status === 'ACTIVO' ? 'success' : 'neutral'}>
                        {selectedProject.status}
                      </StatusBadge>
                    </div>
                  </div>
                  <div>
                    <h4 className="text-xs font-semibold text-muted-foreground">Margen mínimo</h4>
                    <p className="mt-1 font-medium">{((selectedProject.minimumMargin || 0) * 100).toFixed(1)}%</p>
                  </div>
                  <div>
                    <h4 className="text-xs font-semibold text-muted-foreground">Responsable</h4>
                    <p className="mt-1 font-medium">
                      {systemUsersQuery.data?.find((u) => u.id === selectedProject.responsibleUserId)?.username || 'No asignado'}
                    </p>
                  </div>
                </div>
                <div className="flex flex-wrap items-center gap-2 border-t border-border pt-4 text-xs text-muted-foreground">
                  <Calendar className="size-4" aria-hidden="true" />
                  <span>Inicio: {selectedProject.estimatedStartDate || '—'}</span>
                  <span aria-hidden="true">·</span>
                  <span>Fin: {selectedProject.estimatedEndDate || '—'}</span>
                </div>
              </div>

              {/* Team */}
              <div className="space-y-3 border-t border-border pt-4">
                <h3 className="flex items-center gap-1.5 text-sm font-bold">
                  <Users className="size-4 text-primary" aria-hidden="true" />
                  Equipo del proyecto
                </h3>
                <div className="flex gap-2">
                  <select
                    className={selectClass}
                    aria-label="Agregar miembro al equipo"
                    value={teamMemberToAdd}
                    onChange={(e) => setTeamMemberToAdd(e.target.value)}
                  >
                    <option value="">Seleccionar miembro…</option>
                    {systemUsersQuery.data?.map((u) => (
                      <option key={u.id} value={u.id}>{u.username}</option>
                    ))}
                  </select>
                  <Button
                    size="sm"
                    disabled={!teamMemberToAdd || addTeamMemberMutation.isPending}
                    onClick={() =>
                      addTeamMemberMutation.mutate({ params: { path: { id: selectedProject.id! } }, body: { userId: teamMemberToAdd } })
                    }
                  >
                    Agregar
                  </Button>
                </div>
                {projectTeamQuery.isLoading ? (
                  <div className="h-10 w-full animate-pulse rounded-md bg-muted" />
                ) : (
                  <ul className="divide-y divide-border overflow-hidden rounded-lg border border-border bg-background text-xs">
                    {projectTeamQuery.data?.map((member) => (
                      <li key={member.userId} className="flex items-center justify-between px-3 py-2 hover:bg-accent/30">
                        <span className="font-medium">{member.username}</span>
                        <Button
                          variant="ghost"
                          size="icon"
                          className="size-6 text-muted-foreground hover:text-destructive"
                          onClick={() => removeTeamMemberMutation.mutate({ params: { path: { id: selectedProject.id!, userId: member.userId! } } })}
                          disabled={removeTeamMemberMutation.isPending}
                          aria-label={`Remover a ${member.username} del equipo`}
                        >
                          <Trash2 className="size-3" />
                        </Button>
                      </li>
                    ))}
                    {(!projectTeamQuery.data || projectTeamQuery.data.length === 0) ? (
                      <li className="px-3 py-4 text-center text-muted-foreground">Sin miembros adicionales.</li>
                    ) : null}
                  </ul>
                )}
              </div>
            </div>
          ) : (
            <EmptyState
              icon={Briefcase}
              title="Selecciona un proyecto"
              message="Verás aquí su detalle, estado y equipo."
            />
          )}
        </div>
      </div>

      {/* Client drawer */}
      <Drawer open={showClientDrawer} onClose={() => setShowClientDrawer(false)} title="Nuevo cliente" side="right" className="w-full max-w-sm">
        <form onSubmit={clientForm.handleSubmit(handleCreateClient)} className="space-y-4">
          <div>
            <label className={fieldLabel} htmlFor="client-name">Nombre comercial</label>
            <Input id="client-name" placeholder="Ej. Constructora Andina S.A." {...clientForm.register('name')} />
            {clientForm.formState.errors.name ? (
              <p className="mt-1 text-xs text-destructive">{clientForm.formState.errors.name.message}</p>
            ) : null}
          </div>
          <div className="flex justify-end gap-2 border-t border-border pt-3">
            <Button type="button" variant="outline" onClick={() => setShowClientDrawer(false)}>Cancelar</Button>
            <Button type="submit" disabled={createClientMutation.isPending}>Crear cliente</Button>
          </div>
        </form>
      </Drawer>

      {/* Project drawer */}
      <Drawer open={showProjectDrawer} onClose={() => setShowProjectDrawer(false)} title="Nuevo proyecto" side="right" className="w-full max-w-xl">
        <form onSubmit={projectForm.handleSubmit(handleCreateProject)} className="space-y-5">
          <FormSection title="Identificación">
            <CodeField
              id="project-code"
              value={projectForm.watch('code')}
              onChange={(value) => projectForm.setValue('code', value, { shouldValidate: true })}
              onGenerate={generateProjectCode}
              {...(codeError ? { error: codeError } : {})}
            />
            <div>
              <label className={fieldLabel} htmlFor="project-name">Nombre</label>
              <Input id="project-name" placeholder="Nombre del proyecto" {...projectForm.register('name')} />
              {projectForm.formState.errors.name ? (
                <p className="mt-1 text-xs text-destructive">{projectForm.formState.errors.name.message}</p>
              ) : null}
            </div>
          </FormSection>

          <FormSection title="Cliente y responsable">
            <div>
              <div className="mb-1 flex items-center justify-between">
                <label className={fieldLabel + ' mb-0'} htmlFor="project-client">Cliente</label>
                <Button type="button" variant="ghost" size="sm" onClick={() => setShowClientDrawer(true)}>
                  <Plus className="size-3.5" />
                  Nuevo
                </Button>
              </div>
              <select id="project-client" className={selectClass} {...projectForm.register('clientId')}>
                <option value="">Seleccione un cliente…</option>
                {clientsQuery.data?.map((c) => (
                  <option key={c.id} value={c.id}>{c.name}</option>
                ))}
              </select>
              {projectForm.formState.errors.clientId ? (
                <p className="mt-1 text-xs text-destructive">{projectForm.formState.errors.clientId.message}</p>
              ) : null}
            </div>
            <div>
              <label className={fieldLabel} htmlFor="project-responsible">Responsable</label>
              <select id="project-responsible" className={selectClass} {...projectForm.register('responsibleUserId')}>
                <option value="">Sin responsable asignado…</option>
                {systemUsersQuery.data?.map((u) => (
                  <option key={u.id} value={u.id}>{u.username}</option>
                ))}
              </select>
            </div>
          </FormSection>

          <FormSection title="Planificación">
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <div>
                <label className={fieldLabel} htmlFor="project-location">Ubicación</label>
                <Input id="project-location" placeholder="Ej. Quito, Ecuador" {...projectForm.register('location')} />
              </div>
              <div>
                <label className={fieldLabel} htmlFor="project-worktype">Tipo de obra</label>
                <Input id="project-worktype" placeholder="Ej. Edificación" {...projectForm.register('workType')} />
              </div>
              <div>
                <label className={fieldLabel} htmlFor="project-start">Inicio estimado</label>
                <Input id="project-start" type="date" {...projectForm.register('estimatedStartDate')} />
              </div>
              <div>
                <label className={fieldLabel} htmlFor="project-end">Fin estimado</label>
                <Input id="project-end" type="date" {...projectForm.register('estimatedEndDate')} />
              </div>
            </div>
          </FormSection>

          <FormSection title="Valores financieros">
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
              <div>
                <label className={fieldLabel} htmlFor="project-target">Monto objetivo</label>
                <Input id="project-target" type="number" step="0.01" {...projectForm.register('targetAmount')} />
                {projectForm.formState.errors.targetAmount ? (
                  <p className="mt-1 text-xs text-destructive">{projectForm.formState.errors.targetAmount.message}</p>
                ) : null}
              </div>
              <div>
                <label className={fieldLabel} htmlFor="project-margin">Margen mínimo (%)</label>
                <Input id="project-margin" type="number" step="0.1" {...projectForm.register('minimumMargin')} />
                {projectForm.formState.errors.minimumMargin ? (
                  <p className="mt-1 text-xs text-destructive">{projectForm.formState.errors.minimumMargin.message}</p>
                ) : null}
              </div>
              <div>
                <label className={fieldLabel} htmlFor="project-currency">Moneda</label>
                <Input id="project-currency" maxLength={3} {...projectForm.register('currencyCode')} />
              </div>
            </div>
          </FormSection>

          <FormSection title="Descripción">
            <textarea
              aria-label="Descripción"
              className="flex min-h-20 w-full rounded-md border border-border bg-background px-3 py-2 text-sm shadow-xs focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
              placeholder="Detalles adicionales del proyecto…"
              {...projectForm.register('description')}
            />
          </FormSection>

          <div className="flex justify-end gap-2 border-t border-border pt-3">
            <Button type="button" variant="outline" onClick={() => setShowProjectDrawer(false)}>Cancelar</Button>
            <Button type="submit" disabled={createProjectMutation.isPending}>Crear proyecto</Button>
          </div>
        </form>
      </Drawer>
    </div>
  )
}
