import { useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import * as zod from 'zod'
import { toast } from 'sonner'
import {
  FolderHeart,
  History,
  Edit3,
  DollarSign,
  Calculator,
  Sparkles,
  Send,
  Plus,
  RefreshCw,
  ShieldAlert,
  AlertTriangle,
  CheckCircle2,
  XCircle,
  Trash2,
  Info,
  User
} from 'lucide-react'

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

const riskSchema = zod.object({
  description: zod.string().min(3, 'Mínimo 3 caracteres').max(500),
  probability: zod.coerce.number().min(0, 'Mínimo 0%').max(100, 'Máximo 100%'),
  impact: zod.coerce.number().min(0, 'Mínimo 0'),
  assignedTo: zod.string().max(100).optional().or(zod.string().length(0)),
  mitigation: zod.string().max(1000).optional().or(zod.string().length(0)),
  mitigated: zod.boolean().default(false),
})
type RiskFormValues = zod.infer<typeof riskSchema>

interface BudgetRiskDto {
  id?: string
  description?: string
  probability?: number
  impact?: number
  assignedTo?: string | null
  mitigation?: string | null
  mitigated?: boolean
}

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
  const [showRiskModal, setShowRiskModal] = useState(false)
  const [selectedRiskId, setSelectedRiskId] = useState<string | null>(null)
  const [showPriceUpdateModal, setShowPriceUpdateModal] = useState(false)

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

  const risksQuery = useQuery({
    ...api.queryOptions('get', '/api/budget-versions/{versionId}/risks', {
      params: { path: { versionId: selectedVersionId || '' } }
    }),
    enabled: !!selectedVersionId,
  })

  const qualityQuery = useQuery({
    ...api.queryOptions('get', '/api/budget-versions/{versionId}/quality', {
      params: { path: { versionId: selectedVersionId || '' } }
    }),
    enabled: !!selectedVersionId,
  })

  const pricePreviewQuery = useQuery({
    ...api.queryOptions('get', '/api/budget-versions/{versionId}/price-update-preview', {
      params: { path: { versionId: selectedVersionId || '' } }
    }),
    enabled: !!selectedVersionId && showPriceUpdateModal,
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

  const addRiskMutation = api.useMutation('post', '/api/budget-versions/{versionId}/risks', {
    onSuccess: () => {
      toast.success('Riesgo agregado con éxito')
      queryClient.invalidateQueries({
        queryKey: ['get', '/api/budget-versions/{versionId}/risks', { params: { path: { versionId: selectedVersionId! } } }]
      })
      queryClient.invalidateQueries({
        queryKey: ['get', '/api/budget-versions/{versionId}/quality', { params: { path: { versionId: selectedVersionId! } } }]
      })
      setShowRiskModal(false)
      riskForm.reset()
    },
    onError: (err) => toast.error(apiErrorMessage(err, 'Error al agregar riesgo')),
  })

  const updateRiskMutation = api.useMutation('put', '/api/budget-risks/{id}', {
    onSuccess: () => {
      toast.success('Riesgo actualizado con éxito')
      queryClient.invalidateQueries({
        queryKey: ['get', '/api/budget-versions/{versionId}/risks', { params: { path: { versionId: selectedVersionId! } } }]
      })
      queryClient.invalidateQueries({
        queryKey: ['get', '/api/budget-versions/{versionId}/quality', { params: { path: { versionId: selectedVersionId! } } }]
      })
      setShowRiskModal(false)
      setSelectedRiskId(null)
      riskForm.reset()
    },
    onError: (err) => toast.error(apiErrorMessage(err, 'Error al actualizar riesgo')),
  })

  const deleteRiskMutation = api.useMutation('delete', '/api/budget-risks/{id}', {
    onSuccess: () => {
      toast.success('Riesgo eliminado con éxito')
      queryClient.invalidateQueries({
        queryKey: ['get', '/api/budget-versions/{versionId}/risks', { params: { path: { versionId: selectedVersionId! } } }]
      })
      queryClient.invalidateQueries({
        queryKey: ['get', '/api/budget-versions/{versionId}/quality', { params: { path: { versionId: selectedVersionId! } } }]
      })
    },
    onError: (err) => toast.error(apiErrorMessage(err, 'Error al eliminar riesgo')),
  })

  const applyNewPricesMutation = api.useMutation('post', '/api/budget-versions/{versionId}/apply-new-prices', {
    onSuccess: () => {
      toast.success('Precios actualizados y versión recalculada con éxito')
      queryClient.invalidateQueries({
        queryKey: ['get', '/api/budget-versions/{id}', { params: { path: { id: selectedVersionId! } } }]
      })
      queryClient.invalidateQueries({
        queryKey: ['get', '/api/budget-versions/{versionId}/quality', { params: { path: { versionId: selectedVersionId! } } }]
      })
      setShowPriceUpdateModal(false)
    },
    onError: (err) => toast.error(apiErrorMessage(err, 'Error al actualizar precios')),
  })

  const riskForm = useForm({
    resolver: zodResolver(riskSchema),
    defaultValues: {
      description: '',
      probability: 10,
      impact: 0,
      assignedTo: '',
      mitigation: '',
      mitigated: false,
    }
  })

  function handleAddRiskClick() {
    setSelectedRiskId(null)
    riskForm.reset({
      description: '',
      probability: 10,
      impact: 0,
      assignedTo: '',
      mitigation: '',
      mitigated: false,
    })
    setShowRiskModal(true)
  }

  function handleEditRiskClick(risk: BudgetRiskDto) {
    setSelectedRiskId(risk.id ?? null)
    riskForm.reset({
      description: risk.description || '',
      probability: Math.round((risk.probability ?? 0) * 100),
      impact: risk.impact || 0,
      assignedTo: risk.assignedTo || '',
      mitigation: risk.mitigation || '',
      mitigated: !!risk.mitigated,
    })
    setShowRiskModal(true)
  }

  function handleSaveRisk(values: RiskFormValues) {
    if (!selectedVersionId) return
    const probFraction = values.probability / 100
    if (selectedRiskId) {
      updateRiskMutation.mutate({
        params: { path: { id: selectedRiskId } },
        body: {
          description: values.description,
          probability: probFraction,
          impact: values.impact,
          ...(values.assignedTo ? { assignedTo: values.assignedTo } : {}),
          ...(values.mitigation ? { mitigation: values.mitigation } : {}),
          mitigated: values.mitigated,
        }
      })
    } else {
      addRiskMutation.mutate({
        params: { path: { versionId: selectedVersionId } },
        body: {
          description: values.description,
          probability: probFraction,
          impact: values.impact,
          ...(values.assignedTo ? { assignedTo: values.assignedTo } : {}),
          ...(values.mitigation ? { mitigation: values.mitigation } : {}),
          mitigated: values.mitigated,
        }
      })
    }
  }

  function handleDeleteRisk(riskId: string) {
    if (confirm('¿Está seguro de que desea eliminar este riesgo?')) {
      deleteRiskMutation.mutate({
        params: { path: { id: riskId } }
      })
    }
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
                            onClick={() => setShowPriceUpdateModal(true)}
                            disabled={activeVersion.status === 'APROBADO'}
                          >
                            <RefreshCw className="size-4" />
                            Actualizar Precios
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

                      {/* Quality Score & Alerts Panel */}
                      <div className="border border-border bg-background/50 rounded-xl p-4 space-y-4">
                        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
                          <div>
                            <h3 className="font-bold text-sm flex items-center gap-1.5 text-foreground">
                              <ShieldAlert className="size-4 text-primary" />
                              Control de Calidad del Presupuesto
                            </h3>
                            <p className="text-xs text-muted-foreground">Evaluación automatizada de consistencia y completitud.</p>
                          </div>
                          {qualityQuery.data && (
                            <div className="flex items-center gap-2">
                              <span className="text-xs text-muted-foreground font-semibold">Puntuación:</span>
                              <span className={`text-sm font-black px-2.5 py-1 rounded-full ${
                                (qualityQuery.data.score ?? 0) >= 80 ? 'bg-emerald-500/15 text-emerald-500' :
                                (qualityQuery.data.score ?? 0) >= 50 ? 'bg-amber-500/15 text-amber-500' :
                                'bg-rose-500/15 text-rose-500'
                              }`}>
                                {qualityQuery.data.score ?? 0} / 100
                              </span>
                            </div>
                          )}
                        </div>

                        {qualityQuery.isLoading ? (
                          <p className="text-xs text-muted-foreground animate-pulse">Evaluando calidad...</p>
                        ) : qualityQuery.data ? (
                          <div className="grid gap-4 md:grid-cols-2">
                            {/* Checks checklist */}
                            <div className="space-y-2">
                              <h4 className="text-xs font-bold text-muted-foreground uppercase tracking-wider">Reglas Verificadas</h4>
                              <div className="space-y-1.5 max-h-[180px] overflow-y-auto pr-1">
                                {qualityQuery.data.checks?.map((check, idx) => (
                                  <div key={idx} className="flex items-start gap-2 text-xs border border-border/40 bg-panel/45 rounded-lg p-2 transition-all hover:bg-panel/75">
                                    {check.passed ? (
                                      <CheckCircle2 className="size-4 text-emerald-500 shrink-0 mt-0.5" />
                                    ) : (
                                      <XCircle className="size-4 text-rose-500 shrink-0 mt-0.5" />
                                    )}
                                    <div className="flex-1">
                                      <div className="flex justify-between items-center">
                                        <span className="font-semibold text-foreground">{check.name}</span>
                                        {!check.passed && check.penalty !== undefined && check.penalty > 0 && (
                                          <span className="text-[10px] font-bold text-rose-500">-{check.penalty} pts</span>
                                        )}
                                      </div>
                                      <p className="text-[10px] text-muted-foreground mt-0.5">{check.description}</p>
                                    </div>
                                  </div>
                                ))}
                              </div>
                            </div>

                            {/* Alerts */}
                            <div className="space-y-2">
                              <h4 className="text-xs font-bold text-muted-foreground uppercase tracking-wider">Alertas y Sugerencias</h4>
                              <div className="space-y-1.5 max-h-[180px] overflow-y-auto pr-1">
                                {qualityQuery.data.alerts && qualityQuery.data.alerts.length > 0 ? (
                                  qualityQuery.data.alerts.map((alert, idx) => (
                                    <div key={idx} className="flex items-start gap-2 text-xs border border-amber-500/15 bg-amber-500/5 rounded-lg p-2">
                                      <AlertTriangle className="size-4 text-amber-500 shrink-0 mt-0.5" />
                                      <div>
                                        <span className="font-bold text-amber-500 text-[10px] uppercase block">{alert.field}</span>
                                        <p className="text-[10px] text-foreground/90 mt-0.5">{alert.message}</p>
                                      </div>
                                    </div>
                                  ))
                                ) : (
                                  <div className="flex items-center justify-center border border-dashed border-border/60 bg-panel/10 rounded-lg p-6 text-center text-xs text-muted-foreground h-[150px]">
                                    <div>
                                      <CheckCircle2 className="size-6 text-emerald-500/50 mx-auto mb-1.5" />
                                      No se detectaron alertas de calidad.
                                    </div>
                                  </div>
                                )}
                              </div>
                            </div>
                          </div>
                        ) : (
                          <p className="text-xs text-muted-foreground">No se pudo obtener el reporte de calidad.</p>
                        )}
                      </div>

                      {/* Risks Panel */}
                      <div className="border border-border bg-background/50 rounded-xl p-4 space-y-4">
                        <div className="flex justify-between items-center">
                          <div>
                            <h3 className="font-bold text-sm flex items-center gap-1.5 text-foreground">
                              <ShieldAlert className="size-4 text-primary" />
                              Registro de Riesgos y Contingencias
                            </h3>
                            <p className="text-xs text-muted-foreground">Impacto económico esperado y estado de mitigación.</p>
                          </div>
                          {activeVersion.status === 'BORRADOR' && (
                            <Button size="sm" variant="outline" className="h-7 text-xs px-2" onClick={handleAddRiskClick}>
                              <Plus className="size-3.5 mr-1" />
                              Agregar Riesgo
                            </Button>
                          )}
                        </div>

                        {risksQuery.isLoading ? (
                          <p className="text-xs text-muted-foreground animate-pulse">Cargando riesgos...</p>
                        ) : risksQuery.data && risksQuery.data.length > 0 ? (
                          <div className="space-y-3">
                            {/* Risks totals KPI card */}
                            <div className="bg-panel/40 border border-border/80 rounded-lg p-2.5 flex justify-between items-center text-xs">
                              <div>
                                <span className="text-muted-foreground">Total Riesgos: </span>
                                <strong className="text-foreground">{risksQuery.data.length}</strong>
                              </div>
                              <div>
                                <span className="text-muted-foreground">Impacto Esperado Total: </span>
                                <strong className="text-primary font-bold">
                                  $ {risksQuery.data.reduce((sum, r) => sum + (r.expectedAmount || 0), 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                                </strong>
                              </div>
                            </div>

                            {/* Risks list */}
                            <div className="space-y-2 max-h-[250px] overflow-y-auto pr-1">
                              {risksQuery.data.map((risk) => (
                                <div key={risk.id} className="border border-border/60 bg-panel/30 rounded-lg p-3 space-y-2 transition-all hover:bg-panel/60">
                                  <div className="flex justify-between items-start gap-4">
                                    <div className="space-y-1">
                                      <p className="text-xs font-semibold text-foreground">{risk.description}</p>
                                      <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-[10px] text-muted-foreground">
                                        <span className="flex items-center gap-1">
                                          <Info className="size-3" />
                                          Probabilidad: <strong className="text-foreground">{((risk.probability ?? 0) * 100).toFixed(0)}%</strong>
                                        </span>
                                        <span>
                                          Impacto: <strong className="text-foreground">$ {(risk.impact ?? 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</strong>
                                        </span>
                                        <span>
                                          Esperado: <strong className="text-primary font-semibold">$ {(risk.expectedAmount ?? 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</strong>
                                        </span>
                                        {risk.assignedTo && (
                                          <span className="flex items-center gap-1">
                                            <User className="size-3" />
                                            Resp: <strong className="text-foreground">{risk.assignedTo}</strong>
                                          </span>
                                        )}
                                      </div>
                                    </div>
                                    <div className="flex items-center gap-1.5 shrink-0">
                                      <span className={`text-[9px] font-bold px-1.5 py-0.5 rounded-full ${
                                        risk.mitigated ? 'bg-emerald-500/15 text-emerald-500' : 'bg-amber-500/15 text-amber-500'
                                      }`}>
                                        {risk.mitigated ? 'Mitigado' : 'Sin Mitigar'}
                                      </span>
                                      {activeVersion.status === 'BORRADOR' && (
                                        <div className="flex gap-0.5">
                                          <Button variant="ghost" className="h-6 w-6 p-0 [&_svg]:size-3" onClick={() => handleEditRiskClick(risk)}>
                                            <Edit3 className="size-3" />
                                            <span className="sr-only">Editar</span>
                                          </Button>
                                          <Button variant="ghost" className="h-6 w-6 p-0 [&_svg]:size-3 text-muted-foreground hover:text-destructive" onClick={() => handleDeleteRisk(risk.id!)}>
                                            <Trash2 className="size-3" />
                                            <span className="sr-only">Eliminar</span>
                                          </Button>
                                        </div>
                                      )}
                                    </div>
                                  </div>
                                  {risk.mitigation && (
                                    <div className="border-t border-border/40 pt-1.5 mt-1">
                                      <p className="text-[10px] text-muted-foreground italic"><strong className="not-italic text-foreground/80 font-medium">Plan de Mitigación:</strong> {risk.mitigation}</p>
                                    </div>
                                  )}
                                </div>
                              ))}
                            </div>
                          </div>
                        ) : (
                          <div className="flex flex-col items-center justify-center border border-dashed border-border/60 bg-panel/10 rounded-lg p-8 text-center text-xs text-muted-foreground h-32">
                            <ShieldAlert className="size-6 text-muted-foreground/45 mb-1.5" />
                            No se han registrado riesgos para esta revisión de presupuesto.
                          </div>
                        )}
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

      {/* Risk Create / Edit Modal */}
      {showRiskModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs">
          <div className="w-full max-w-md bg-panel border border-border p-6 rounded-xl shadow-lg space-y-4 animate-scale-up">
            <h2 className="text-lg font-bold border-b border-border pb-2">
              {selectedRiskId ? 'Editar Registro de Riesgo' : 'Registrar Nuevo Riesgo'}
            </h2>
            <form onSubmit={riskForm.handleSubmit((v) => handleSaveRisk(v as RiskFormValues))} className="space-y-4">
              <div>
                <label className="text-xs font-semibold text-muted-foreground mb-1 block">Descripción del Riesgo *</label>
                <Input placeholder="Ej. Incremento del precio del acero estructural" {...riskForm.register('description')} />
                {riskForm.formState.errors.description && (
                  <p className="mt-1 text-xs text-destructive">{riskForm.formState.errors.description.message}</p>
                )}
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="text-xs font-semibold text-muted-foreground mb-1 block">Probabilidad (%) *</label>
                  <Input type="number" min="0" max="100" placeholder="Ej. 30" {...riskForm.register('probability')} />
                  {riskForm.formState.errors.probability && (
                    <p className="mt-1 text-xs text-destructive">{riskForm.formState.errors.probability.message}</p>
                  )}
                </div>
                <div>
                  <label className="text-xs font-semibold text-muted-foreground mb-1 block">Impacto Financiero ($) *</label>
                  <Input type="number" step="0.01" min="0" placeholder="Ej. 15000" {...riskForm.register('impact')} />
                  {riskForm.formState.errors.impact && (
                    <p className="mt-1 text-xs text-destructive">{riskForm.formState.errors.impact.message}</p>
                  )}
                </div>
              </div>

              <div>
                <label className="text-xs font-semibold text-muted-foreground mb-1 block">Responsable Asignado</label>
                <Input placeholder="Ej. Director de Obra" {...riskForm.register('assignedTo')} />
                {riskForm.formState.errors.assignedTo && (
                  <p className="mt-1 text-xs text-destructive">{riskForm.formState.errors.assignedTo.message}</p>
                )}
              </div>

              <div>
                <label className="text-xs font-semibold text-muted-foreground mb-1 block">Plan de Mitigación / Contingencia</label>
                <textarea
                  className="flex min-h-16 w-full rounded-md border border-border bg-background px-3 py-2 text-sm shadow-xs focus-visible:outline-hidden"
                  placeholder="Detalle las acciones para evitar o reducir este riesgo..."
                  {...riskForm.register('mitigation')}
                />
                {riskForm.formState.errors.mitigation && (
                  <p className="mt-1 text-xs text-destructive">{riskForm.formState.errors.mitigation.message}</p>
                )}
              </div>

              <div className="flex items-center gap-2 pt-1">
                <input
                  type="checkbox"
                  id="mitigated"
                  className="rounded border-border text-primary focus:ring-primary size-4"
                  {...riskForm.register('mitigated')}
                />
                <label htmlFor="mitigated" className="text-xs font-semibold text-foreground cursor-pointer">
                  Marcar este riesgo como Mitigado
                </label>
              </div>

              <div className="flex justify-end gap-2 border-t border-border pt-3">
                <Button type="button" variant="outline" onClick={() => setShowRiskModal(false)}>
                  Cancelar
                </Button>
                <Button type="submit" disabled={addRiskMutation.isPending || updateRiskMutation.isPending}>
                  {selectedRiskId ? 'Actualizar Riesgo' : 'Agregar Riesgo'}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Price Update Preview Modal */}
      {showPriceUpdateModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs">
          <div className="w-full max-w-3xl bg-panel border border-border p-6 rounded-xl shadow-lg space-y-4 animate-scale-up">
            <div className="flex justify-between items-start border-b border-border pb-2">
              <div>
                <h2 className="text-lg font-bold">Actualización Controlada de Precios</h2>
                <p className="text-xs text-muted-foreground">Compara los precios vigentes de catálogo con los de esta versión.</p>
              </div>
              <Button size="sm" variant="ghost" className="h-7 w-7 p-0" onClick={() => setShowPriceUpdateModal(false)}>
                <XCircle className="size-5" />
                <span className="sr-only">Cerrar</span>
              </Button>
            </div>

            {pricePreviewQuery.isLoading ? (
              <div className="py-12 text-center text-xs text-muted-foreground animate-pulse">
                <RefreshCw className="size-8 animate-spin mx-auto mb-2 text-primary" />
                Analizando variaciones de precios y recalculando impacto...
              </div>
            ) : pricePreviewQuery.data ? (
              <div className="space-y-4">
                {pricePreviewQuery.data.changes && pricePreviewQuery.data.changes.length > 0 ? (
                  <>
                    <div className="border border-border rounded-lg overflow-hidden bg-background/30">
                      <div className="max-h-60 overflow-y-auto">
                        <table className="w-full text-left text-xs border-collapse">
                          <thead className="bg-muted/40 sticky top-0 border-b border-border z-10">
                            <tr>
                              <th className="p-2.5 font-semibold text-muted-foreground">Insumo / Rubro</th>
                              <th className="p-2.5 font-semibold text-muted-foreground text-right">Precio Actual</th>
                              <th className="p-2.5 font-semibold text-muted-foreground text-right">Precio Nuevo</th>
                              <th className="p-2.5 font-semibold text-muted-foreground text-right">Diferencia</th>
                              <th className="p-2.5 font-semibold text-muted-foreground text-right">Impacto Total</th>
                            </tr>
                          </thead>
                          <tbody className="divide-y divide-border/60">
                            {pricePreviewQuery.data.changes.map((change, idx) => (
                              <tr key={idx} className="hover:bg-accent/30 transition-colors">
                                <td className="p-2.5">
                                  <span className="font-mono text-[9px] text-muted-foreground block">{change.insumoCode}</span>
                                  <span className="font-medium text-foreground block max-w-xs truncate">{change.insumoName}</span>
                                  <span className="text-[10px] text-muted-foreground truncate block max-w-xs">{change.componentDescription}</span>
                                </td>
                                <td className="p-2.5 text-right font-mono">$ {(change.oldPrice ?? 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</td>
                                <td className="p-2.5 text-right font-mono">$ {(change.newPrice ?? 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</td>
                                <td className={`p-2.5 text-right font-mono font-semibold ${
                                  (change.difference ?? 0) > 0 ? 'text-rose-500' : (change.difference ?? 0) < 0 ? 'text-emerald-500' : 'text-muted-foreground'
                                }`}>
                                  {(change.difference ?? 0) > 0 ? '+' : ''}{(change.difference ?? 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                                </td>
                                <td className={`p-2.5 text-right font-mono font-bold ${
                                  (change.lineTotalDifference ?? 0) > 0 ? 'text-rose-500' : (change.lineTotalDifference ?? 0) < 0 ? 'text-emerald-500' : 'text-muted-foreground'
                                }`}>
                                  {(change.lineTotalDifference ?? 0) > 0 ? '+' : ''}{(change.lineTotalDifference ?? 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                                </td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    </div>

                    {/* Impact summary cards */}
                    <div className="grid grid-cols-3 gap-4 bg-muted/20 border border-border/80 rounded-xl p-3 text-center">
                      <div>
                        <span className="text-[10px] font-medium text-muted-foreground uppercase">Costo Actual</span>
                        <p className="font-bold text-sm mt-0.5 text-foreground">$ {(pricePreviewQuery.data.currentTotalCost ?? 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</p>
                      </div>
                      <div>
                        <span className="text-[10px] font-medium text-muted-foreground uppercase">Costo Propuesto</span>
                        <p className="font-bold text-sm mt-0.5 text-foreground">$ {(pricePreviewQuery.data.proposedTotalCost ?? 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</p>
                      </div>
                      <div>
                        <span className="text-[10px] font-medium text-muted-foreground uppercase">Desviación Total</span>
                        <p className={`font-black text-sm mt-0.5 ${
                          (pricePreviewQuery.data.difference ?? 0) > 0 ? 'text-rose-500' : (pricePreviewQuery.data.difference ?? 0) < 0 ? 'text-emerald-500' : 'text-muted-foreground'
                        }`}>
                          {(pricePreviewQuery.data.difference ?? 0) > 0 ? '+' : ''}{(pricePreviewQuery.data.difference ?? 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                        </p>
                      </div>
                    </div>
                  </>
                ) : (
                  <div className="flex flex-col items-center justify-center border border-dashed border-border bg-panel/20 rounded-xl p-12 text-center text-xs text-muted-foreground h-48">
                    <CheckCircle2 className="size-8 text-emerald-500/50 mb-2" />
                    Todos los precios de esta versión están al día con las vigencias de catálogo. No se requieren cambios.
                  </div>
                )}

                <div className="flex justify-end gap-2 border-t border-border pt-3">
                  <Button type="button" variant="outline" onClick={() => setShowPriceUpdateModal(false)}>
                    Cerrar
                  </Button>
                  {pricePreviewQuery.data.changes && pricePreviewQuery.data.changes.length > 0 && (
                    <Button
                      type="button"
                      disabled={applyNewPricesMutation.isPending}
                      onClick={() => applyNewPricesMutation.mutate({ params: { path: { versionId: selectedVersionId! } } })}
                    >
                      {applyNewPricesMutation.isPending ? 'Aplicando...' : 'Aplicar Actualización'}
                    </Button>
                  )}
                </div>
              </div>
            ) : (
              <div className="py-6 text-center text-xs text-muted-foreground">
                Error al cargar la previsualización de precios.
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  )
}
