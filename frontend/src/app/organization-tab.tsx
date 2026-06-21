import { useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import * as zod from 'zod'
import { toast } from 'sonner'
import { Shield, ShieldCheck, UserPlus, Users, ToggleLeft, ToggleRight, Trash2, Plus, Check } from 'lucide-react'

import { api } from '@/lib/api/client'
import { apiErrorMessage } from '@/lib/api/errors'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { useSessionStore } from '@/lib/auth/session-store'
import { usePermission } from '@/lib/auth/permissions'

const addMemberSchema = zod.object({
  usernameOrEmail: zod.string().min(3, 'Debe tener al menos 3 caracteres'),
  role: zod.string().min(1, 'Selecciona un rol'),
})

type AddMemberFormValues = zod.infer<typeof addMemberSchema>

const createUserSchema = zod.object({
  username: zod.string().min(3, 'Mínimo 3 caracteres').max(80),
  email: zod.string().email('Email inválido').or(zod.string().length(0)),
  password: zod.string().min(6, 'Mínimo 6 caracteres'),
})

type CreateUserFormValues = zod.infer<typeof createUserSchema>

export function OrganizationTab() {
  const queryClient = useQueryClient()
  const activeOrgId = useSessionStore((state) => state.activeOrganizationId)
  // UX-only gate: the backend @PreAuthorize('ADMINISTRACION_WRITE') remains the authority.
  const canWrite = usePermission('ADMINISTRACION_WRITE')

  // Members list in organization
  const membersQuery = useQuery({
    ...api.queryOptions('get', '/api/members'),
    enabled: !!activeOrgId,
  })

  // System-wide users list
  const usersQuery = useQuery({
    ...api.queryOptions('get', '/api/admin/users'),
    enabled: !!activeOrgId,
    retry: false,
  })

  // RBAC: roles (system + custom) and the registered permission catalog.
  const rolesQuery = useQuery({
    ...api.queryOptions('get', '/api/roles'),
    enabled: !!activeOrgId,
  })
  const permissionsQuery = useQuery({
    ...api.queryOptions('get', '/api/permissions'),
    enabled: !!activeOrgId,
  })

  const [newRoleCode, setNewRoleCode] = useState('')
  const [newRoleName, setNewRoleName] = useState('')
  const [newRolePerms, setNewRolePerms] = useState<string[]>([])

  function toggleNewRolePerm(code: string) {
    setNewRolePerms((current) =>
      current.includes(code) ? current.filter((c) => c !== code) : [...current, code],
    )
  }

  const createRoleMutation = api.useMutation('post', '/api/roles', {
    onSuccess: () => {
      toast.success('Rol personalizado creado')
      queryClient.invalidateQueries({ queryKey: ['get', '/api/roles'] })
      setNewRoleCode('')
      setNewRoleName('')
      setNewRolePerms([])
    },
    onError: (err) => {
      toast.error(apiErrorMessage(err, 'Error al crear el rol'))
    },
  })

  const deleteRoleMutation = api.useMutation('delete', '/api/roles/{id}', {
    onSuccess: () => {
      toast.success('Rol eliminado')
      queryClient.invalidateQueries({ queryKey: ['get', '/api/roles'] })
    },
    onError: (err) => {
      toast.error(apiErrorMessage(err, 'Error al eliminar el rol'))
    },
  })

  // Mutations using openapi-react-query hooks directly
  const addMemberMutation = api.useMutation('post', '/api/members', {
    onSuccess: () => {
      toast.success('Miembro agregado correctamente')
      queryClient.invalidateQueries({ queryKey: ['get', '/api/members'] })
      addMemberForm.reset()
    },
    onError: (err) => {
      toast.error(apiErrorMessage(err, 'Error al agregar miembro'))
    },
  })

  const removeMemberMutation = api.useMutation('delete', '/api/members/{id}', {
    onSuccess: () => {
      toast.success('Miembro eliminado')
      queryClient.invalidateQueries({ queryKey: ['get', '/api/members'] })
    },
    onError: (err) => {
      toast.error(apiErrorMessage(err, 'Error al eliminar miembro'))
    },
  })

  const toggleUserMutation = api.useMutation('put', '/api/admin/users/{id}/toggle', {
    onSuccess: () => {
      toast.success('Estado de usuario modificado')
      queryClient.invalidateQueries({ queryKey: ['get', '/api/admin/users'] })
      queryClient.invalidateQueries({ queryKey: ['get', '/api/members'] })
    },
    onError: (err) => {
      toast.error(apiErrorMessage(err, 'Error al modificar usuario'))
    },
  })

  const createUserMutation = api.useMutation('post', '/api/admin/users', {
    onSuccess: () => {
      toast.success('Cuenta de usuario creada')
      queryClient.invalidateQueries({ queryKey: ['get', '/api/admin/users'] })
      createUserForm.reset()
    },
    onError: (err) => {
      toast.error(apiErrorMessage(err, 'Error al crear usuario'))
    },
  })

  // Forms without explicit generic parameters
  const addMemberForm = useForm({
    resolver: zodResolver(addMemberSchema),
    defaultValues: {
      usernameOrEmail: '',
      role: 'PRESUPUESTISTA' as const,
    },
  })

  const createUserForm = useForm({
    resolver: zodResolver(createUserSchema),
    defaultValues: {
      username: '',
      email: '',
      password: '',
    },
  })

  function onAddMember(values: AddMemberFormValues) {
    addMemberMutation.mutate({
      body: {
        usernameOrEmail: values.usernameOrEmail,
        role: values.role,
      },
    })
  }

  function onCreateUser(values: CreateUserFormValues) {
    createUserMutation.mutate({
      body: {
        username: values.username,
        password: values.password,
        ...(values.email ? { email: values.email } : {})
      },
    })
  }

  return (
    <div className="space-y-8 animate-fade-in">
      <div className="flex flex-col gap-2 border-b border-border pb-4">
        <h1 className="text-2xl font-bold tracking-tight">Organización y Miembros</h1>
        <p className="text-sm text-muted-foreground">
          Administra las membresías de tu organización, asigna roles de acceso y gestiona cuentas de usuario.
        </p>
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        {/* Members Management */}
        <div className="rounded-xl border border-border bg-panel p-5 space-y-6">
          <div className="flex items-center gap-2 border-b border-border pb-3">
            <Users className="size-5 text-primary" />
            <h2 className="text-lg font-semibold">Miembros de la Organización</h2>
          </div>

          {canWrite && (
          <form onSubmit={addMemberForm.handleSubmit(onAddMember)} className="flex flex-col sm:flex-row gap-2">
            <div className="flex-1">
              <Input
                placeholder="Nombre de usuario o Email"
                {...addMemberForm.register('usernameOrEmail')}
                aria-label="Nombre de usuario o Email"
              />
              {addMemberForm.formState.errors.usernameOrEmail && (
                <p className="mt-1 text-xs text-destructive">{addMemberForm.formState.errors.usernameOrEmail.message}</p>
              )}
            </div>
            <div>
              <select
                className="flex h-9 w-full rounded-md border border-border bg-background px-3 py-1 text-sm shadow-xs focus-visible:outline-hidden focus-visible:ring-2 focus-visible:ring-ring"
                {...addMemberForm.register('role')}
                aria-label="Rol"
              >
                {rolesQuery.data?.map((role) => (
                  <option key={role.id} value={role.code}>
                    {role.name}
                  </option>
                ))}
              </select>
            </div>
            <Button type="submit" disabled={addMemberMutation.isPending}>
              <UserPlus className="size-4" />
              Invitar
            </Button>
          </form>
          )}

          {membersQuery.isLoading ? (
            <div className="space-y-2 py-4">
              <div className="h-10 w-full animate-pulse bg-muted rounded-md" />
              <div className="h-10 w-full animate-pulse bg-muted rounded-md" />
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm border-collapse">
                <thead>
                  <tr className="border-b border-border text-muted-foreground text-xs font-semibold">
                    <th className="py-2">Usuario</th>
                    <th className="py-2">Email</th>
                    <th className="py-2">Rol</th>
                    <th className="py-2 text-right">Acciones</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-border">
                  {membersQuery.data?.map((member) => (
                    <tr key={member.id} className="hover:bg-accent/40 transition-colors">
                      <td className="py-3 font-medium">{member.username}</td>
                      <td className="py-3 text-muted-foreground">{member.email || '-'}</td>
                      <td className="py-3">
                        <span className="inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-medium bg-primary/10 text-primary">
                          <Shield className="size-3" />
                          {member.role}
                        </span>
                      </td>
                      <td className="py-3 text-right">
                        {canWrite && (
                        <Button
                          variant="ghost"
                          size="icon"
                          className="hover:text-destructive hover:bg-destructive/10"
                          onClick={() => removeMemberMutation.mutate({ params: { path: { id: member.id! } } })}
                          disabled={removeMemberMutation.isPending}
                          aria-label={`Eliminar miembro ${member.username}`}
                        >
                          <Trash2 className="size-4" />
                        </Button>
                        )}
                      </td>
                    </tr>
                  ))}
                  {(!membersQuery.data || membersQuery.data.length === 0) && (
                    <tr>
                      <td colSpan={4} className="py-8 text-center text-muted-foreground">
                        No hay miembros en esta organización.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          )}
        </div>

        {/* Global User Accounts Administration */}
        <div className="rounded-xl border border-border bg-panel p-5 space-y-6">
          <div className="flex items-center gap-2 border-b border-border pb-3">
            <Shield className="size-5 text-primary" />
            <h2 className="text-lg font-semibold">Administración de Usuarios (Global)</h2>
          </div>

          {canWrite && (
          <form onSubmit={createUserForm.handleSubmit(onCreateUser)} className="grid grid-cols-1 sm:grid-cols-3 gap-2 items-end">
            <div>
              <label className="text-xs font-medium text-muted-foreground mb-1 block">Usuario</label>
              <Input
                placeholder="Nombre de usuario"
                {...createUserForm.register('username')}
              />
              {createUserForm.formState.errors.username && (
                <p className="mt-1 text-xs text-destructive">{createUserForm.formState.errors.username.message}</p>
              )}
            </div>
            <div>
              <label className="text-xs font-medium text-muted-foreground mb-1 block">Email</label>
              <Input
                placeholder="Email (opcional)"
                {...createUserForm.register('email')}
              />
              {createUserForm.formState.errors.email && (
                <p className="mt-1 text-xs text-destructive">{createUserForm.formState.errors.email.message}</p>
              )}
            </div>
            <div className="flex gap-2">
              <div className="flex-1">
                <label className="text-xs font-medium text-muted-foreground mb-1 block">Contraseña</label>
                <Input
                  type="password"
                  placeholder="Contraseña"
                  {...createUserForm.register('password')}
                />
                {createUserForm.formState.errors.password && (
                  <p className="mt-1 text-xs text-destructive">{createUserForm.formState.errors.password.message}</p>
                )}
              </div>
              <Button type="submit" disabled={createUserMutation.isPending} className="self-end h-9">
                Crear
              </Button>
            </div>
          </form>
          )}

          {usersQuery.isLoading ? (
            <div className="space-y-2 py-4">
              <div className="h-10 w-full animate-pulse bg-muted rounded-md" />
              <div className="h-10 w-full animate-pulse bg-muted rounded-md" />
            </div>
          ) : usersQuery.isError ? (
            <div className="py-8 text-center text-muted-foreground border border-dashed border-border rounded-lg">
              No tienes permisos globales de administrador para ver o administrar todas las cuentas de usuario del sistema.
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm border-collapse">
                <thead>
                  <tr className="border-b border-border text-muted-foreground text-xs font-semibold">
                    <th className="py-2">Usuario</th>
                    <th className="py-2">Email</th>
                    <th className="py-2">Estado</th>
                    <th className="py-2 text-right">Acción</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-border">
                  {usersQuery.data?.map((u) => (
                    <tr key={u.id} className="hover:bg-accent/40 transition-colors">
                      <td className="py-3 font-medium">{u.username}</td>
                      <td className="py-3 text-muted-foreground">{u.email || '-'}</td>
                      <td className="py-3">
                        <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${u.enabled ? 'bg-emerald-500/10 text-emerald-500' : 'bg-destructive/10 text-destructive'}`}>
                          {u.enabled ? 'Activo' : 'Inactivo'}
                        </span>
                      </td>
                      <td className="py-3 text-right">
                        {canWrite ? (
                        <Button
                          variant="ghost"
                          size="icon"
                          onClick={() => toggleUserMutation.mutate({ params: { path: { id: u.id! } } })}
                          disabled={toggleUserMutation.isPending}
                          aria-label={`Toggle user active state`}
                        >
                          {u.enabled ? (
                            <ToggleRight className="size-6 text-emerald-500" />
                          ) : (
                            <ToggleLeft className="size-6 text-muted-foreground" />
                          )}
                        </Button>
                        ) : null}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>

      {/* Roles & Permissions (RBAC) */}
      <div className="rounded-xl border border-border bg-panel p-5 space-y-6">
        <div className="flex items-center gap-2 border-b border-border pb-3">
          <ShieldCheck className="size-5 text-primary" />
          <h2 className="text-lg font-semibold">Roles y Permisos</h2>
        </div>

        {/* Create custom role */}
        {canWrite && (
        <div className="rounded-lg border border-border/80 bg-background p-4 space-y-3">
          <p className="text-sm font-semibold">Crear rol personalizado</p>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
            <Input
              placeholder="Código (ej. REVISOR)"
              value={newRoleCode}
              onChange={(e) => setNewRoleCode(e.target.value)}
              aria-label="Código del rol"
            />
            <Input
              placeholder="Nombre visible"
              value={newRoleName}
              onChange={(e) => setNewRoleName(e.target.value)}
              aria-label="Nombre del rol"
            />
          </div>
          <div className="flex flex-wrap gap-2">
            {permissionsQuery.data?.map((perm) => {
              const selected = newRolePerms.includes(perm.code!)
              return (
                <button
                  type="button"
                  key={perm.code}
                  onClick={() => toggleNewRolePerm(perm.code!)}
                  aria-pressed={selected}
                  title={perm.description}
                  className={`inline-flex items-center gap-1 rounded-full border px-2.5 py-1 text-xs font-medium transition-colors ${
                    selected
                      ? 'border-primary bg-primary/10 text-primary'
                      : 'border-border text-muted-foreground hover:bg-accent/40'
                  }`}
                >
                  {selected && <Check className="size-3" />}
                  {perm.code}
                </button>
              )
            })}
          </div>
          <Button
            size="sm"
            disabled={createRoleMutation.isPending || !newRoleCode.trim() || !newRoleName.trim()}
            onClick={() =>
              createRoleMutation.mutate({
                body: { code: newRoleCode.trim(), name: newRoleName.trim(), permissions: newRolePerms },
              })
            }
          >
            <Plus className="size-4" />
            Crear rol
          </Button>
        </div>
        )}

        {/* Permission matrix */}
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs border-collapse">
            <thead>
              <tr className="border-b border-border text-muted-foreground font-semibold">
                <th className="py-2 pr-4 sticky left-0 bg-panel">Permiso</th>
                {rolesQuery.data?.map((role) => (
                  <th key={role.id} className="py-2 px-2 text-center whitespace-nowrap">
                    <div className="flex flex-col items-center gap-1">
                      <span>{role.name}</span>
                      {role.system ? (
                        <span className="text-[10px] font-normal text-muted-foreground">sistema</span>
                      ) : canWrite ? (
                        <button
                          type="button"
                          onClick={() => deleteRoleMutation.mutate({ params: { path: { id: role.id! } } })}
                          disabled={deleteRoleMutation.isPending}
                          className="text-[10px] font-normal text-destructive hover:underline inline-flex items-center gap-0.5"
                          aria-label={`Eliminar rol ${role.name}`}
                        >
                          <Trash2 className="size-3" /> eliminar
                        </button>
                      ) : (
                        <span className="text-[10px] font-normal text-muted-foreground">personalizado</span>
                      )}
                    </div>
                  </th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-border">
              {permissionsQuery.data?.map((perm) => (
                <tr key={perm.code} className="hover:bg-accent/40 transition-colors">
                  <td className="py-2 pr-4 sticky left-0 bg-panel" title={perm.description}>
                    <span className="font-medium">{perm.code}</span>
                  </td>
                  {rolesQuery.data?.map((role) => {
                    const has = role.allPermissions || role.permissions?.includes(perm.code!)
                    return (
                      <td key={role.id} className="py-2 px-2 text-center">
                        {has ? (
                          <Check className="size-4 text-emerald-500 inline" aria-label="permitido" />
                        ) : (
                          <span className="text-muted-foreground/40" aria-label="sin permiso">—</span>
                        )}
                      </td>
                    )
                  })}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}
