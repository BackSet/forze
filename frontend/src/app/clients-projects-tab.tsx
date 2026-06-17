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

export function ClientsProjectsTab() {
  const queryClient = useQueryClient()
  const activeOrgId = useSessionStore((state) => state.activeOrganizationId)
  const [selectedProjectId, setSelectedProjectId] = useState<string | null>(null)
  const [showClientModal, setShowClientModal] = useState(false)
  const [showProjectModal, setShowProjectModal] = useState(false)
  const [searchQuery, setSearchQuery] = useState('')

  // Queries
  const clientsQuery = useQuery({
    ...api.queryOptions('get', '/api/clients'),
    enabled: !!activeOrgId,
  })

  const projectsQuery = useQuery({
    ...api.queryOptions('get', '/api/projects'),
    enabled: !!activeOrgId,
  })

  const systemUsersQuery = useQuery({
    ...api.queryOptions('get', '/api/admin/users'),
    enabled: !!activeOrgId,
  })

  const projectDetailsQuery = useQuery({
    ...api.queryOptions('get', '/api/projects/{id}', { params: { path: { id: selectedProjectId || '' } } }),
    enabled: !!selectedProjectId,
  })

  const projectTeamQuery = useQuery({
    ...api.queryOptions('get', '/api/projects/{id}/team', { params: { path: { id: selectedProjectId || '' } } }),
    enabled: !!selectedProjectId,
  })

  // Mutations using openapi-react-query hooks directly
  const createClientMutation = api.useMutation('post', '/api/clients', {
    onSuccess: () => {
      toast.success('Cliente creado con éxito')
      queryClient.invalidateQueries({ queryKey: ['get', '/api/clients'] })
      setShowClientModal(false)
      clientForm.reset()
    },
    onError: (err) => {
      toast.error(apiErrorMessage(err, 'Error al crear cliente'))
    },
  })

  const createProjectMutation = api.useMutation('post', '/api/projects', {
    onSuccess: () => {
      toast.success('Proyecto creado con éxito')
      queryClient.invalidateQueries({ queryKey: ['get', '/api/projects'] })
      setShowProjectModal(false)
      projectForm.reset()
    },
    onError: (err) => {
      toast.error(apiErrorMessage(err, 'Error al crear proyecto'))
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
    onError: (err) => {
      toast.error(apiErrorMessage(err, 'Error al archivar proyecto'))
    },
  })

  const addTeamMemberMutation = api.useMutation('post', '/api/projects/{id}/team', {
    onSuccess: () => {
      toast.success('Miembro de equipo agregado')
      queryClient.invalidateQueries({ queryKey: ['get', '/api/projects/{id}/team', { params: { path: { id: selectedProjectId! } } }] })
    },
    onError: (err) => {
      toast.error(apiErrorMessage(err, 'Error al agregar miembro'))
    },
  })

  const removeTeamMemberMutation = api.useMutation('delete', '/api/projects/{id}/team/{userId}', {
    onSuccess: () => {
      toast.success('Miembro de equipo removido')
      queryClient.invalidateQueries({ queryKey: ['get', '/api/projects/{id}/team', { params: { path: { id: selectedProjectId! } } }] })
    },
    onError: (err) => {
      toast.error(apiErrorMessage(err, 'Error al remover miembro'))
    },
  })

  // Forms
  const clientForm = useForm({
    resolver: zodResolver(clientSchema) as unknown as Resolver<ClientFormValues>,
    defaultValues: { name: '' },
  })

  const projectForm = useForm({
    resolver: zodResolver(projectSchema) as unknown as Resolver<ProjectFormValues>,
    defaultValues: {
      code: '',
      name: '',
      clientId: '',
      description: '',
      workType: '',
      location: '',
      estimatedStartDate: '',
      estimatedEndDate: '',
      currencyCode: 'USD',
      targetAmount: 0,
      minimumMargin: 10,
      responsibleUserId: '',
    },
  })

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

  // Filter projects
  const filteredProjects = projectsQuery.data?.filter(p =>
    p.name?.toLowerCase().includes(searchQuery.toLowerCase()) ||
    p.code?.toLowerCase().includes(searchQuery.toLowerCase())
  )

  const selectedProject = projectDetailsQuery.data

  return (
    <div className="space-y-6 animate-fade-in">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 border-b border-border pb-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Clientes y Proyectos</h1>
          <p className="text-sm text-muted-foreground">Administra los clientes comerciales y crea presupuestos por proyecto.</p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" onClick={() => setShowClientModal(true)}>
            <Building className="size-4" />
            Nuevo Cliente
          </Button>
          <Button onClick={() => setShowProjectModal(true)}>
            <Plus className="size-4" />
            Nuevo Proyecto
          </Button>
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-[1fr_360px]">
        {/* Projects List */}
        <div className="space-y-4">
          <div className="flex items-center gap-2">
            <Input
              placeholder="Buscar proyectos por código o nombre..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="max-w-md bg-panel"
            />
          </div>

          {projectsQuery.isLoading ? (
            <div className="space-y-3">
              <div className="h-20 w-full animate-pulse bg-muted rounded-xl" />
              <div className="h-20 w-full animate-pulse bg-muted rounded-xl" />
            </div>
          ) : (
            <div className="grid gap-3 sm:grid-cols-1">
              {filteredProjects?.map((project) => (
                <div
                  key={project.id}
                  onClick={() => setSelectedProjectId(project.id!)}
                  className={`group relative flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 rounded-xl border p-4 cursor-pointer transition-all hover:bg-accent/40 ${
                    selectedProjectId === project.id ? 'border-primary bg-primary/5 shadow-xs' : 'border-border bg-panel'
                  }`}
                >
                  <div className="space-y-1">
                    <div className="flex items-center gap-2">
                      <span className="text-xs font-mono font-bold bg-secondary px-2 py-0.5 rounded text-secondary-foreground">
                        {project.code}
                      </span>
                      <h3 className="font-semibold text-base leading-none group-hover:text-primary transition-colors">
                        {project.name}
                      </h3>
                    </div>
                    <p className="text-xs text-muted-foreground">
                      Cliente: {clientsQuery.data?.find(c => c.id === project.clientId)?.name || 'Cargando...'}
                    </p>
                    {project.location && (
                      <p className="text-xs text-muted-foreground font-medium">Ubicación: {project.location}</p>
                    )}
                  </div>

                  <div className="flex items-center gap-6 self-stretch sm:self-auto justify-between sm:justify-end border-t sm:border-t-0 pt-2 sm:pt-0 border-border">
                    <div className="text-right sm:text-right">
                      <p className="text-xs text-muted-foreground">Monto Objetivo</p>
                      <p className="font-bold text-sm text-foreground">
                        {project.currencyCode} {project.targetAmount?.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                      </p>
                    </div>
                    <ChevronRight className="size-5 text-muted-foreground group-hover:translate-x-1 transition-transform" />
                  </div>
                </div>
              ))}
              {(!filteredProjects || filteredProjects.length === 0) && (
                <div className="py-12 text-center text-muted-foreground border border-dashed border-border rounded-xl">
                  No se encontraron proyectos. Crea uno nuevo para comenzar.
                </div>
              )}
            </div>
          )}
        </div>

        {/* Project Details Panel */}
        <div className="space-y-6">
          {selectedProjectId && selectedProject ? (
            <div className="rounded-xl border border-border bg-panel p-5 space-y-6 animate-fade-in">
              <div className="flex justify-between items-start border-b border-border pb-3">
                <div>
                  <div className="text-xs font-mono font-semibold text-muted-foreground">{selectedProject.code}</div>
                  <h2 className="text-lg font-bold text-foreground">{selectedProject.name}</h2>
                </div>
                <div className="flex gap-1">
                  {selectedProject.status !== 'ARCHIVADO' && (
                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={() => archiveProjectMutation.mutate({ params: { path: { id: selectedProject.id! } } })}
                      disabled={archiveProjectMutation.isPending}
                      title="Archivar Proyecto"
                    >
                      <Archive className="size-4 text-muted-foreground hover:text-destructive" />
                    </Button>
                  )}
                </div>
              </div>

              <div className="space-y-4 text-sm">
                {selectedProject.description && (
                  <div>
                    <h4 className="text-xs font-semibold text-muted-foreground">Descripción</h4>
                    <p className="mt-1 text-foreground">{selectedProject.description}</p>
                  </div>
                )}

                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <h4 className="text-xs font-semibold text-muted-foreground">Tipo de Obra</h4>
                    <p className="mt-1 text-foreground font-medium">{selectedProject.workType || '-'}</p>
                  </div>
                  <div>
                    <h4 className="text-xs font-semibold text-muted-foreground">Estado</h4>
                    <span className={`mt-1 inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${
                      selectedProject.status === 'ACTIVO' ? 'bg-emerald-500/10 text-emerald-500' : 'bg-destructive/10 text-destructive'
                    }`}>
                      {selectedProject.status}
                    </span>
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <h4 className="text-xs font-semibold text-muted-foreground">Margen Mínimo</h4>
                    <p className="mt-1 text-foreground font-medium">
                      {((selectedProject.minimumMargin || 0) * 100).toFixed(1)}%
                    </p>
                  </div>
                  <div>
                    <h4 className="text-xs font-semibold text-muted-foreground">Responsable</h4>
                    <p className="mt-1 text-foreground font-medium">
                      {systemUsersQuery.data?.find(u => u.id === selectedProject.responsibleUserId)?.username || 'No asignado'}
                    </p>
                  </div>
                </div>

                <div className="flex items-center gap-2 border-t border-border pt-4 text-xs text-muted-foreground">
                  <Calendar className="size-4" />
                  <span>Inicio: {selectedProject.estimatedStartDate || '-'}</span>
                  <span>|</span>
                  <span>Fin: {selectedProject.estimatedEndDate || '-'}</span>
                </div>
              </div>

              {/* Project Team */}
              <div className="border-t border-border pt-4 space-y-3">
                <div className="flex justify-between items-center">
                  <h3 className="text-sm font-bold flex items-center gap-1">
                    <Users className="size-4 text-primary" />
                    Equipo del Proyecto
                  </h3>
                </div>

                {/* Invite to team */}
                <div className="flex gap-2">
                  <select
                    id="teamMemberSelect"
                    className="flex h-9 flex-1 rounded-md border border-border bg-background px-3 py-1 text-sm shadow-xs focus-visible:outline-hidden focus-visible:ring-2 focus-visible:ring-ring"
                    aria-label="Agregar miembro"
                  >
                    <option value="">Seleccionar miembro...</option>
                    {systemUsersQuery.data?.map(u => (
                      <option key={u.id} value={u.id}>{u.username}</option>
                    ))}
                  </select>
                  <Button
                    size="sm"
                    onClick={() => {
                      const select = document.getElementById('teamMemberSelect') as HTMLSelectElement
                      const userId = select.value
                      if (userId) {
                        addTeamMemberMutation.mutate({
                          params: { path: { id: selectedProject.id! } },
                          body: { userId },
                        })
                        select.value = ''
                      }
                    }}
                  >
                    Agregar
                  </Button>
                </div>

                {projectTeamQuery.isLoading ? (
                  <div className="h-10 w-full animate-pulse bg-muted rounded-md" />
                ) : (
                  <ul className="divide-y divide-border text-xs border border-border rounded-lg bg-background overflow-hidden">
                    {projectTeamQuery.data?.map(member => (
                      <li key={member.userId} className="flex justify-between items-center px-3 py-2 hover:bg-accent/30">
                        <span className="font-medium text-foreground">{member.username}</span>
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
                    {(!projectTeamQuery.data || projectTeamQuery.data.length === 0) && (
                      <li className="px-3 py-4 text-center text-muted-foreground">
                        No hay miembros adicionales asignados a este proyecto.
                      </li>
                    )}
                  </ul>
                )}
              </div>
            </div>
          ) : (
            <div className="rounded-xl border border-dashed border-border bg-panel/30 p-8 text-center text-muted-foreground flex flex-col items-center justify-center h-48">
              <Briefcase className="size-8 text-muted-foreground/50 mb-2" />
              Selecciona un proyecto para ver sus detalles, miembros de equipo y estado.
            </div>
          )}
        </div>
      </div>

      {/* Client Modal */}
      {showClientModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs">
          <div className="w-full max-w-md bg-panel border border-border p-6 rounded-xl shadow-lg space-y-4 animate-scale-up">
            <h2 className="text-lg font-bold border-b border-border pb-2">Crear Comercial Cliente</h2>
            <form onSubmit={clientForm.handleSubmit((v) => handleCreateClient(v))} className="space-y-4">
              <div>
                <label className="text-sm font-medium text-muted-foreground mb-1 block">Nombre Comercial</label>
                <Input placeholder="Ej. Constructora Andina S.A." {...clientForm.register('name')} />
                {clientForm.formState.errors.name && (
                  <p className="mt-1 text-xs text-destructive">{clientForm.formState.errors.name.message}</p>
                )}
              </div>
              <div className="flex justify-end gap-2 border-t border-border pt-3">
                <Button type="button" variant="outline" onClick={() => setShowClientModal(false)}>
                  Cancelar
                </Button>
                <Button type="submit" disabled={createClientMutation.isPending}>
                  Crear Cliente
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Project Modal */}
      {showProjectModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs overflow-y-auto">
          <div className="w-full max-w-lg bg-panel border border-border p-6 rounded-xl shadow-lg my-8 space-y-4 animate-scale-up">
            <h2 className="text-lg font-bold border-b border-border pb-2">Crear Nuevo Proyecto</h2>
            <form onSubmit={projectForm.handleSubmit((v) => handleCreateProject(v))} className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="text-sm font-medium text-muted-foreground mb-1 block">Código</label>
                  <Input placeholder="Ej. PRY-2026-01" {...projectForm.register('code')} />
                  {projectForm.formState.errors.code && (
                    <p className="mt-1 text-xs text-destructive">{projectForm.formState.errors.code.message}</p>
                  )}
                </div>
                <div>
                  <label className="text-sm font-medium text-muted-foreground mb-1 block">Nombre</label>
                  <Input placeholder="Nombre del proyecto" {...projectForm.register('name')} />
                  {projectForm.formState.errors.name && (
                    <p className="mt-1 text-xs text-destructive">{projectForm.formState.errors.name.message}</p>
                  )}
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="text-sm font-medium text-muted-foreground mb-1 block">Cliente</label>
                  <select
                    className="flex h-9 w-full rounded-md border border-border bg-background px-3 py-1 text-sm shadow-xs focus-visible:outline-hidden"
                    {...projectForm.register('clientId')}
                  >
                    <option value="">Seleccione un cliente...</option>
                    {clientsQuery.data?.map(c => (
                      <option key={c.id} value={c.id}>{c.name}</option>
                    ))}
                  </select>
                  {projectForm.formState.errors.clientId && (
                    <p className="mt-1 text-xs text-destructive">{projectForm.formState.errors.clientId.message}</p>
                  )}
                </div>
                <div>
                  <label className="text-sm font-medium text-muted-foreground mb-1 block">Responsable del Proyecto</label>
                  <select
                    className="flex h-9 w-full rounded-md border border-border bg-background px-3 py-1 text-sm shadow-xs focus-visible:outline-hidden"
                    {...projectForm.register('responsibleUserId')}
                  >
                    <option value="">Sin responsable asignado...</option>
                    {systemUsersQuery.data?.map(u => (
                      <option key={u.id} value={u.id}>{u.username}</option>
                    ))}
                  </select>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="text-sm font-medium text-muted-foreground mb-1 block">Monto Objetivo</label>
                  <Input type="number" step="0.01" {...projectForm.register('targetAmount')} />
                  {projectForm.formState.errors.targetAmount && (
                    <p className="mt-1 text-xs text-destructive">{projectForm.formState.errors.targetAmount.message}</p>
                  )}
                </div>
                <div>
                  <label className="text-sm font-medium text-muted-foreground mb-1 block">Margen Mínimo Aceptable (%)</label>
                  <Input type="number" step="0.1" {...projectForm.register('minimumMargin')} />
                  {projectForm.formState.errors.minimumMargin && (
                    <p className="mt-1 text-xs text-destructive">{projectForm.formState.errors.minimumMargin.message}</p>
                  )}
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="text-sm font-medium text-muted-foreground mb-1 block">Ubicación</label>
                  <Input placeholder="Ej. Quito, Ecuador" {...projectForm.register('location')} />
                </div>
                <div>
                  <label className="text-sm font-medium text-muted-foreground mb-1 block">Tipo de Obra</label>
                  <Input placeholder="Ej. Vialidad / Edificación" {...projectForm.register('workType')} />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="text-sm font-medium text-muted-foreground mb-1 block">Fecha Estimada Inicio</label>
                  <Input type="date" {...projectForm.register('estimatedStartDate')} />
                </div>
                <div>
                  <label className="text-sm font-medium text-muted-foreground mb-1 block">Fecha Estimada Fin</label>
                  <Input type="date" {...projectForm.register('estimatedEndDate')} />
                </div>
              </div>

              <div>
                <label className="text-sm font-medium text-muted-foreground mb-1 block">Descripción</label>
                <textarea
                  className="flex min-h-20 w-full rounded-md border border-border bg-background px-3 py-2 text-sm shadow-xs focus-visible:outline-hidden"
                  placeholder="Detalles adicionales del proyecto..."
                  {...projectForm.register('description')}
                />
              </div>

              <div className="flex justify-end gap-2 border-t border-border pt-3">
                <Button type="button" variant="outline" onClick={() => setShowProjectModal(false)}>
                  Cancelar
                </Button>
                <Button type="submit" disabled={createProjectMutation.isPending}>
                  Crear Proyecto
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}
