import { useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import * as zod from 'zod'
import { toast } from 'sonner'
import { FolderHeart, History, Edit3, DollarSign, Calculator, Sparkles, Send, Plus } from 'lucide-react'

import { api } from '@/lib/api/client'
import { apiErrorMessage } from '@/lib/api/errors'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'

const budgetSchema = zod.object({
  code: zod.string().min(2, 'Mínimo 2 caracteres').max(60),
  name: zod.string().min(3, 'Mínimo 3 caracteres').max(200),
  currencyCode: zod.string().length(3).default('USD'),
})
type BudgetFormValues = zod.infer<typeof budgetSchema>

const versionSchema = zod.object({
  name: zod.string().min(3, 'Mínimo 3 caracteres').max(200),
  changeReason: zod.string().optional(),
})
type VersionFormValues = zod.infer<typeof versionSchema>

const financialsSchema = zod.object({
  targetAmount: zod.coerce.number().min(0),
  utilityRate: zod.coerce.number().min(0).max(100),
  indirectRate: zod.coerce.number().min(0).max(100),
  contingencyRate: zod.coerce.number().min(0).max(100),
  taxConfigId: zod.string().uuid('Seleccione impuesto').optional().or(zod.string().length(0)),
  validUntil: zod.string().optional(),
})
type FinancialsFormValues = zod.infer<typeof financialsSchema>

interface BudgetsTabProps {
  selectedProjectId: string | null
  setSelectedProjectId: (id: string | null) => void
  selectedBudgetId: string | null
  setSelectedBudgetId: (id: string | null) => void
  selectedVersionId: string | null
  setSelectedVersionId: (id: string | null) => void
  setActiveTab: (tab: string) => void
}

export function BudgetsTab({
  selectedProjectId,
  setSelectedProjectId,
  selectedBudgetId,
  setSelectedBudgetId,
  selectedVersionId,
  setSelectedVersionId,
  setActiveTab,
}: BudgetsTabProps) {
  const queryClient = useQueryClient()
  
  const [showBudgetModal, setShowBudgetModal] = useState(false)
  const [showVersionModal, setShowVersionModal] = useState(false)
  const [showFinancialsModal, setShowFinancialsModal] = useState(false)

  // Queries
  const projectsQuery = useQuery(api.queryOptions('get', '/api/projects'))
  
  const budgetsQuery = useQuery({
    ...api.queryOptions('get', '/api/projects/{projectId}/budgets', {
      params: { path: { projectId: selectedProjectId || '' } }
    }),
    enabled: !!selectedProjectId,
  })

  const versionsQuery = useQuery({
    ...api.queryOptions('get', '/api/budgets/{budgetId}/versions', {
      params: { path: { budgetId: selectedBudgetId || '' } }
    }),
    enabled: !!selectedBudgetId,
  })

  const activeVersionQuery = useQuery({
    ...api.queryOptions('get', '/api/budget-versions/{id}', {
      params: { path: { id: selectedVersionId || '' } }
    }),
    enabled: !!selectedVersionId,
  })

  const taxesQuery = useQuery(api.queryOptions('get', '/api/admin/taxes'))

  // Mutations
  const createBudgetMutation = api.useMutation('post', '/api/projects/{projectId}/budgets', {
    onSuccess: (res) => {
      toast.success('Presupuesto creado con éxito')
      queryClient.invalidateQueries({
        queryKey: ['get', '/api/projects/{projectId}/budgets', { params: { path: { projectId: selectedProjectId! } } }]
      })
      setSelectedBudgetId(res.id!)
      setSelectedVersionId(null)
      setShowBudgetModal(false)
      budgetForm.reset()
    },
    onError: (err) => toast.error(apiErrorMessage(err, 'Error al crear presupuesto')),
  })

  const copyVersionMutation = api.useMutation('post', '/api/budgets/{budgetId}/versions', {
    onSuccess: (res) => {
      toast.success('Nueva versión/revisión de presupuesto creada')
      queryClient.invalidateQueries({
        queryKey: ['get', '/api/budgets/{budgetId}/versions', { params: { path: { budgetId: selectedBudgetId! } } }]
      })
      setSelectedVersionId(res.id!)
      setShowVersionModal(false)
      versionForm.reset()
    },
    onError: (err) => toast.error(apiErrorMessage(err, 'Error al copiar versión')),
  })

  const configureFinancialsMutation = api.useMutation('put', '/api/budget-versions/{id}/financials', {
    onSuccess: () => {
      toast.success('Tasas financieras actualizadas')
      queryClient.invalidateQueries({
        queryKey: ['get', '/api/budget-versions/{id}', { params: { path: { id: selectedVersionId! } } }]
      })
      queryClient.invalidateQueries({
        queryKey: ['get', '/api/budgets/{budgetId}/versions', { params: { path: { budgetId: selectedBudgetId! } } }]
      })
      setShowFinancialsModal(false)
    },
    onError: (err) => toast.error(apiErrorMessage(err, 'Error al configurar financieras')),
  })

  const calculateVersionMutation = api.useMutation('post', '/api/budget-versions/{id}/calculate', {
    onSuccess: () => {
      toast.success('Costos calculados correctamente')
      queryClient.invalidateQueries({
        queryKey: ['get', '/api/budget-versions/{id}', { params: { path: { id: selectedVersionId! } } }]
      })
      queryClient.invalidateQueries({
        queryKey: ['get', '/api/budgets/{budgetId}/versions', { params: { path: { budgetId: selectedBudgetId! } } }]
      })
    },
    onError: (err) => toast.error(apiErrorMessage(err, 'Error al calcular presupuestos')),
  })

  const submitApprovalMutation = api.useMutation('post', '/api/budget-versions/{versionId}/approvals', {
    onSuccess: () => {
      toast.success('Presupuesto enviado a aprobación')
      queryClient.invalidateQueries({
        queryKey: ['get', '/api/budget-versions/{id}', { params: { path: { id: selectedVersionId! } } }]
      })
      queryClient.invalidateQueries({
        queryKey: ['get', '/api/budgets/{budgetId}/versions', { params: { path: { budgetId: selectedBudgetId! } } }]
      })
    },
    onError: (err) => toast.error(apiErrorMessage(err, 'Error al enviar a aprobación')),
  })

  // Forms
  const budgetForm = useForm({
    resolver: zodResolver(budgetSchema),
    defaultValues: { code: '', name: '', currencyCode: 'USD' }
  })

  const versionForm = useForm({
    resolver: zodResolver(versionSchema),
    defaultValues: { name: '', changeReason: '' }
  })

  const financialsForm = useForm({
    resolver: zodResolver(financialsSchema),
    defaultValues: { targetAmount: 0, utilityRate: 10, indirectRate: 15, contingencyRate: 5, taxConfigId: '', validUntil: '' }
  })

  function handleCreateBudget(values: BudgetFormValues) {
    if (!selectedProjectId) return
    createBudgetMutation.mutate({
      params: { path: { projectId: selectedProjectId } },
      body: { code: values.code, name: values.name, currencyCode: values.currencyCode }
    })
  }

  function handleCreateVersion(values: VersionFormValues) {
    if (!selectedBudgetId || !selectedVersionId) return
    copyVersionMutation.mutate({
      params: { path: { budgetId: selectedBudgetId } },
      body: {
        baseVersionId: selectedVersionId,
        name: values.name,
        ...(values.changeReason ? { changeReason: values.changeReason } : {})
      }
    })
  }

  function handleConfigureFinancials(values: FinancialsFormValues) {
    if (!selectedVersionId) return
    configureFinancialsMutation.mutate({
      params: { path: { id: selectedVersionId } },
      body: {
        targetAmount: values.targetAmount,
        utilityRate: values.utilityRate / 100, // Fractional rate
        indirectRate: values.indirectRate / 100,
        contingencyRate: values.contingencyRate / 100,
        ...(values.taxConfigId ? { taxConfigId: values.taxConfigId } : {}),
        ...(values.validUntil ? { validUntil: values.validUntil } : {}),
      }
    })
  }

  const activeVersion = activeVersionQuery.data

  return (
    <div className="space-y-6 animate-fade-in">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 border-b border-border pb-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight font-sans">Presupuestos del Proyecto</h1>
          <p className="text-sm text-muted-foreground">Administra las revisiones de presupuestos, configura indirectos y calcula costos de obra.</p>
        </div>
        <div className="flex gap-2 w-full sm:w-auto">
          <select
            className="flex h-9 flex-1 sm:w-64 rounded-md border border-border bg-background px-3 py-1 text-sm shadow-xs focus-visible:outline-hidden"
            value={selectedProjectId || ''}
            onChange={(e) => {
              setSelectedProjectId(e.target.value || null)
              setSelectedBudgetId(null)
              setSelectedVersionId(null)
            }}
            aria-label="Seleccionar proyecto"
          >
            <option value="">Seleccione Proyecto...</option>
            {projectsQuery.data?.map(p => (
              <option key={p.id} value={p.id}>[{p.code}] {p.name}</option>
            ))}
          </select>

          {selectedProjectId && (
            <Button size="sm" onClick={() => setShowBudgetModal(true)}>
              <Plus className="size-4" />
              Nuevo Presupuesto
            </Button>
          )}
        </div>
      </div>

      {selectedProjectId ? (
        <div className="grid gap-6 lg:grid-cols-[280px_1fr]">
          {/* Budgets list */}
          <div className="rounded-xl border border-border bg-panel p-4 space-y-4">
            <h3 className="font-bold text-sm border-b border-border pb-2 flex items-center gap-1.5">
              <FolderHeart className="size-4 text-primary" />
              Presupuestos
            </h3>
            <ul className="space-y-1">
              {budgetsQuery.data?.map(b => (
                <li
                  key={b.id}
                  onClick={() => {
                    setSelectedBudgetId(b.id!)
                    setSelectedVersionId(null)
                  }}
                  className={`px-3 py-2 rounded-lg cursor-pointer text-xs font-semibold border transition-all ${
                    selectedBudgetId === b.id ? 'bg-primary/10 border-primary/20 text-primary' : 'border-transparent text-foreground hover:bg-accent/40'
                  }`}
                >
                  <span className="font-mono block text-[10px]">{b.code}</span>
                  <span className="truncate block">{b.name}</span>
                </li>
              ))}
              {(!budgetsQuery.data || budgetsQuery.data.length === 0) && (
                <p className="text-center text-xs text-muted-foreground py-6">No hay presupuestos creados.</p>
              )}
            </ul>
          </div>

          {/* Budget Versions details */}
          <div className="space-y-6">
            {selectedBudgetId ? (
              <div className="grid gap-6 md:grid-cols-[250px_1fr]">
                {/* Versions History list */}
                <div className="rounded-xl border border-border bg-panel p-4 space-y-3">
                  <h3 className="font-bold text-sm border-b border-border pb-2 flex items-center gap-1.5">
                    <History className="size-4 text-primary" />
                    Versiones / Revisiones
                  </h3>
                  <ul className="space-y-1 max-h-80 overflow-y-auto">
                    {versionsQuery.data?.map(v => (
                      <li
                        key={v.id}
                        onClick={() => setSelectedVersionId(v.id!)}
                        className={`px-3 py-2 rounded-lg cursor-pointer text-xs font-medium border transition-all flex justify-between items-center ${
                          selectedVersionId === v.id ? 'bg-primary/15 border-primary/30 text-primary' : 'border-transparent text-foreground hover:bg-accent/40'
                        }`}
                      >
                        <div>
                          <span className="block font-bold">Rev {v.versionNumber}</span>
                          <span className="block text-[10px] text-muted-foreground truncate max-w-36">{v.name}</span>
                        </div>
                        <span className={`text-[9px] rounded-full px-1.5 py-0.5 font-bold ${
                          v.status === 'APROBADO' ? 'bg-emerald-500/15 text-emerald-500' : 'bg-secondary text-secondary-foreground'
                        }`}>
                          {v.status}
                        </span>
                      </li>
                    ))}
                    {(!versionsQuery.data || versionsQuery.data.length === 0) && (
                      <p className="text-center text-xs text-muted-foreground py-6">Sin versiones. Crea una nueva.</p>
                    )}
                  </ul>
                  {selectedVersionId && (
                    <Button variant="outline" size="sm" className="w-full text-xs" onClick={() => setShowVersionModal(true)}>
                      + Nueva Versión (Copia)
                    </Button>
                  )}
                </div>

                {/* Selected Version Detail */}
                <div>
                  {selectedVersionId && activeVersion ? (
                    <div className="rounded-xl border border-border bg-panel p-5 space-y-6 animate-fade-in">
                      <div className="flex justify-between items-start border-b border-border pb-3">
                        <div>
                          <span className="text-xs text-muted-foreground font-semibold">Versión Rev {activeVersion.versionNumber}</span>
                          <h2 className="text-lg font-bold">{activeVersion.name}</h2>
                        </div>
                        <div className="flex gap-2">
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => {
                              financialsForm.setValue('targetAmount', activeVersion.targetAmount || 0)
                              financialsForm.setValue('utilityRate', (activeVersion.utilityRate || 0) * 100)
                              financialsForm.setValue('indirectRate', (activeVersion.indirectRate || 0) * 100)
                              financialsForm.setValue('contingencyRate', (activeVersion.contingencyRate || 0) * 100)
                              setShowFinancialsModal(true)
                            }}
                            disabled={activeVersion.status === 'APROBADO'}
                          >
                            Tasas
                          </Button>
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => calculateVersionMutation.mutate({ params: { path: { id: selectedVersionId! } } })}
                            disabled={calculateVersionMutation.isPending || activeVersion.status === 'APROBADO'}
                          >
                            <Calculator className="size-4" />
                            Calcular
                          </Button>
                          <Button
                            size="sm"
                            onClick={() => setActiveTab('editor')}
                          >
                            <Edit3 className="size-4" />
                            Editor
                          </Button>
                        </div>
                      </div>

                      {/* Financial Card KPI layout */}
                      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                        <div className="border border-border/80 bg-background rounded-lg p-3 text-center">
                          <span className="text-[10px] font-medium text-muted-foreground uppercase">Costo Directo</span>
                          <p className="font-bold text-base mt-1 text-foreground">
                            $ {activeVersion.totalCost?.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 }) || '0.00'}
                          </p>
                        </div>
                        <div className="border border-border/80 bg-background rounded-lg p-3 text-center">
                          <span className="text-[10px] font-medium text-muted-foreground uppercase">Precio Venta</span>
                          <p className="font-bold text-base mt-1 text-primary">
                            $ {activeVersion.salePrice?.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 }) || '0.00'}
                          </p>
                        </div>
                        <div className="border border-border/80 bg-background rounded-lg p-3 text-center">
                          <span className="text-[10px] font-medium text-muted-foreground uppercase">Margen Obra</span>
                          <p className="font-bold text-base mt-1 text-foreground">
                            {((activeVersion.margin || 0) * 100).toFixed(1)}%
                          </p>
                        </div>
                        <div className="border border-border/80 bg-background rounded-lg p-3 text-center">
                          <span className="text-[10px] font-medium text-muted-foreground uppercase">Viabilidad</span>
                          <span className={`block font-bold text-sm mt-1.5 ${
                            activeVersion.viabilityStatus === 'VIABLE' ? 'text-emerald-500' : 'text-amber-500 font-semibold'
                          }`}>
                            {activeVersion.viabilityStatus || 'PENDIENTE'}
                          </span>
                        </div>
                      </div>

                      <div className="border-t border-border pt-4 flex justify-between items-center text-xs">
                        <span className="text-muted-foreground">Estado de aprobación: <strong>{activeVersion.status}</strong></span>
                        {activeVersion.status === 'BORRADOR' && (
                          <Button
                            size="sm"
                            variant="secondary"
                            className="bg-emerald-500/10 text-emerald-500 hover:bg-emerald-500/20"
                            onClick={() => submitApprovalMutation.mutate({ params: { path: { versionId: selectedVersionId! } } })}
                            disabled={submitApprovalMutation.isPending}
                          >
                            <Send className="size-3.5" />
                            Enviar a Aprobación
                          </Button>
                        )}
                      </div>
                    </div>
                  ) : (
                    <div className="rounded-xl border border-dashed border-border bg-panel/30 p-12 text-center text-muted-foreground h-64 flex flex-col items-center justify-center">
                      <Sparkles className="size-8 text-muted-foreground/45 mb-2" />
                      Selecciona una revisión o versión para configurar financieros, ejecutar cálculos y enviar a aprobación.
                    </div>
                  )}
                </div>
              </div>
            ) : (
              <div className="rounded-xl border border-dashed border-border bg-panel/30 p-12 text-center text-muted-foreground h-64 flex flex-col items-center justify-center">
                <DollarSign className="size-8 text-muted-foreground/45 mb-2" />
                Selecciona un presupuesto comercial para ver sus versiones de obra y cálculo financiero.
              </div>
            )}
          </div>
        </div>
      ) : (
        <div className="rounded-xl border border-dashed border-border bg-panel/30 p-12 text-center text-muted-foreground h-64 flex flex-col items-center justify-center">
          <Sparkles className="size-8 text-muted-foreground/45 mb-2" />
          Selecciona un proyecto del menú superior para comenzar a administrar sus presupuestos y costeo de obra.
        </div>
      )}

      {/* Budget Create Modal */}
      {showBudgetModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs">
          <div className="w-full max-w-md bg-panel border border-border p-6 rounded-xl shadow-lg space-y-4 animate-scale-up">
            <h2 className="text-lg font-bold border-b border-border pb-2">Crear Presupuesto Comercial</h2>
            <form onSubmit={budgetForm.handleSubmit((v) => handleCreateBudget(v))} className="space-y-4">
              <div>
                <label className="text-xs font-semibold text-muted-foreground mb-1 block">Código</label>
                <Input placeholder="Ej. B-01" {...budgetForm.register('code')} />
                {budgetForm.formState.errors.code && (
                  <p className="mt-1 text-xs text-destructive">{budgetForm.formState.errors.code.message}</p>
                )}
              </div>
              <div>
                <label className="text-xs font-semibold text-muted-foreground mb-1 block">Nombre / Detalle de Presupuesto</label>
                <Input placeholder="Presupuesto Base de Licitación" {...budgetForm.register('name')} />
                {budgetForm.formState.errors.name && (
                  <p className="mt-1 text-xs text-destructive">{budgetForm.formState.errors.name.message}</p>
                )}
              </div>

              <div className="flex justify-end gap-2 border-t border-border pt-3">
                <Button type="button" variant="outline" onClick={() => setShowBudgetModal(false)}>
                  Cancelar
                </Button>
                <Button type="submit" disabled={createBudgetMutation.isPending}>
                  Crear Presupuesto
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Version Copy Modal */}
      {showVersionModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs">
          <div className="w-full max-w-md bg-panel border border-border p-6 rounded-xl shadow-lg space-y-4 animate-scale-up">
            <h2 className="text-lg font-bold border-b border-border pb-2">Crear Nueva Revisión (Copia)</h2>
            <form onSubmit={versionForm.handleSubmit((v) => handleCreateVersion(v))} className="space-y-4">
              <div>
                <label className="text-xs font-semibold text-muted-foreground mb-1 block">Nombre de Revisión</label>
                <Input placeholder="Ej. Optimización de Estructuras" {...versionForm.register('name')} />
                {versionForm.formState.errors.name && (
                  <p className="mt-1 text-xs text-destructive">{versionForm.formState.errors.name.message}</p>
                )}
              </div>
              <div>
                <label className="text-xs font-semibold text-muted-foreground mb-1 block">Motivo del Cambio</label>
                <textarea
                  className="flex min-h-16 w-full rounded-md border border-border bg-background px-3 py-2 text-sm shadow-xs focus-visible:outline-hidden"
                  placeholder="Detalle el porqué de esta revisión..."
                  {...versionForm.register('changeReason')}
                />
              </div>

              <div className="flex justify-end gap-2 border-t border-border pt-3">
                <Button type="button" variant="outline" onClick={() => setShowVersionModal(false)}>
                  Cancelar
                </Button>
                <Button type="submit" disabled={copyVersionMutation.isPending}>
                  Copiar Versión
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Financials Settings Modal */}
      {showFinancialsModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs">
          <div className="w-full max-w-md bg-panel border border-border p-6 rounded-xl shadow-lg space-y-4 animate-scale-up">
            <h2 className="text-lg font-bold border-b border-border pb-2">Configurar Tasas y Ratios Financieros</h2>
            <form onSubmit={financialsForm.handleSubmit((v) => handleConfigureFinancials(v))} className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="text-xs font-semibold text-muted-foreground mb-1 block">Monto Objetivo Licitado</label>
                  <Input type="number" step="0.01" {...financialsForm.register('targetAmount')} />
                </div>
                <div>
                  <label className="text-xs font-semibold text-muted-foreground mb-1 block">Impuesto de Venta</label>
                  <select
                    className="flex h-9 w-full rounded-md border border-border bg-background px-3 py-1 text-sm shadow-xs focus-visible:outline-hidden"
                    {...financialsForm.register('taxConfigId')}
                  >
                    <option value="">Seleccione...</option>
                    {taxesQuery.data?.map(t => (
                      <option key={t.id} value={t.id}>{t.name} ({((t.rate || 0)*100).toFixed(0)}%)</option>
                    ))}
                  </select>
                </div>
              </div>

              <div className="grid grid-cols-3 gap-3">
                <div>
                  <label className="text-xs font-semibold text-muted-foreground mb-1 block">Utilidad %</label>
                  <Input type="number" step="0.01" {...financialsForm.register('utilityRate')} />
                </div>
                <div>
                  <label className="text-xs font-semibold text-muted-foreground mb-1 block">Costos Indir. %</label>
                  <Input type="number" step="0.01" {...financialsForm.register('indirectRate')} />
                </div>
                <div>
                  <label className="text-xs font-semibold text-muted-foreground mb-1 block">Contingencia %</label>
                  <Input type="number" step="0.01" {...financialsForm.register('contingencyRate')} />
                </div>
              </div>

              <div className="flex justify-end gap-2 border-t border-border pt-3">
                <Button type="button" variant="outline" onClick={() => setShowFinancialsModal(false)}>
                  Cancelar
                </Button>
                <Button type="submit" disabled={configureFinancialsMutation.isPending}>
                  Guardar Tasas
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}
