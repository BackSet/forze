import { useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm, type Resolver } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import * as zod from 'zod'
import { toast } from 'sonner'
import { FolderPlus, Ruler, Settings, Trash2, ShieldAlert, Sparkles, Plus } from 'lucide-react'

import { api } from '@/lib/api/client'
import { apiErrorMessage } from '@/lib/api/errors'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { StatusBadge } from '@/components/ui/status-badge'
import { ConfirmAction } from '@/components/ui/confirm-action'

function viabilityTone(status: string | undefined): 'success' | 'warning' | 'danger' | 'neutral' {
  if (status === 'VIABLE') return 'success'
  if (status === 'VIABLE_CON_ALERTAS') return 'warning'
  if (status === 'NO_VIABLE') return 'danger'
  return 'neutral'
}

const chapterSchema = zod.object({
  name: zod.string().min(3, 'Mínimo 3 caracteres').max(200),
  parentId: zod.string().uuid().optional().or(zod.string().length(0)),
})
type ChapterFormValues = zod.infer<typeof chapterSchema>

const itemSchema = zod.object({
  rubroId: zod.string().uuid('Seleccione rubro del catálogo'),
  quantity: zod.coerce.number().min(0.0001, 'Cantidad debe ser mayor a 0'),
})
type ItemFormValues = zod.infer<typeof itemSchema>

const measurementSchema = zod.object({
  description: zod.string().min(2, 'Requerido'),
  length: zod.coerce.number().optional().default(1),
  width: zod.coerce.number().optional().default(1),
  height: zod.coerce.number().optional().default(1),
  itemCount: zod.coerce.number().optional().default(1),
  factor: zod.coerce.number().optional().default(1),
  formula: zod.string().optional(),
  notes: zod.string().optional(),
})
type MeasurementFormValues = zod.infer<typeof measurementSchema>

const apuYieldSchema = zod.object({
  yield: zod.coerce.number().min(0.0001, 'Debe ser mayor a 0'),
})

const apuCompSchema = zod.object({
  section: zod.enum(['MATERIALES', 'MANO_DE_OBRA', 'EQUIPOS', 'TRANSPORTE', 'SUBCONTRATOS', 'OTROS']),
  insumoId: zod.string().uuid('Seleccione insumo').optional().or(zod.string().length(0)),
  description: zod.string().optional(),
  unitId: zod.string().uuid('Seleccione unidad'),
  quantity: zod.coerce.number().min(0),
  yield: zod.coerce.number().min(0).optional(),
  wasteFactor: zod.coerce.number().min(0).default(0),
  unitPrice: zod.coerce.number().min(0).default(0),
})

interface EditorTabProps {
  selectedVersionId: string | null
}

export function EditorTab({ selectedVersionId }: EditorTabProps) {
  const queryClient = useQueryClient()
  
  const [selectedBudgetItemId, setSelectedBudgetItemId] = useState<string | null>(null)
  const [rightPanelTab, setRightPanelTab] = useState<'measurements' | 'apu'>('measurements')
  
  const [showChapterModal, setShowChapterModal] = useState(false)
  const [showItemModal, setShowItemModal] = useState(false)
  const [selectedChapterIdForAddingItem, setSelectedChapterIdForAddingItem] = useState<string | null>(null)
  const [showApuCompModal, setShowApuCompModal] = useState(false)

  // Queries
  const versionQuery = useQuery({
    ...api.queryOptions('get', '/api/budget-versions/{id}', { params: { path: { id: selectedVersionId || '' } } }),
    enabled: !!selectedVersionId,
  })

  const chaptersQuery = useQuery({
    ...api.queryOptions('get', '/api/budget-versions/{versionId}/chapters', { params: { path: { versionId: selectedVersionId || '' } } }),
    enabled: !!selectedVersionId,
  })

  const itemsQuery = useQuery({
    ...api.queryOptions('get', '/api/budget-versions/{versionId}/items', { params: { path: { versionId: selectedVersionId || '' } } }),
    enabled: !!selectedVersionId,
  })

  const rubrosCatalogQuery = useQuery(api.queryOptions('get', '/api/rubros'))
  const insumosCatalogQuery = useQuery(api.queryOptions('get', '/api/insumos'))
  const unitsQuery = useQuery(api.queryOptions('get', '/api/admin/units'))

  // Selected Item details queries
  const selectedItem = itemsQuery.data?.find(i => i.id === selectedBudgetItemId)

  const apuQuery = useQuery({
    ...api.queryOptions('get', '/api/budget-items/{id}/apu', { params: { path: { id: selectedBudgetItemId || '' } } }),
    enabled: !!selectedBudgetItemId,
  })

  const selectedItemApuId = apuQuery.data?.id

  const apuComponentsQuery = useQuery({
    ...api.queryOptions('get', '/api/item-apus/{apuId}/components', { params: { path: { apuId: selectedItemApuId || '' } } }),
    enabled: !!selectedItemApuId,
  })

  const measurementsQuery = useQuery({
    ...api.queryOptions('get', '/api/budget-items/{id}/measurements', { params: { path: { id: selectedBudgetItemId || '' } } }),
    enabled: !!selectedBudgetItemId,
  })

  // Mutations
  const createChapterMutation = api.useMutation('post', '/api/budget-versions/{versionId}/chapters', {
    onSuccess: () => {
      toast.success('Capítulo creado')
      queryClient.invalidateQueries({
        queryKey: ['get', '/api/budget-versions/{versionId}/chapters', { params: { path: { versionId: selectedVersionId! } } }]
      })
      setShowChapterModal(false)
      chapterForm.reset()
    },
    onError: (err) => toast.error(apiErrorMessage(err, 'Error al crear capítulo')),
  })

  const addItemMutation = api.useMutation('post', '/api/budget-versions/{versionId}/items', {
    onSuccess: (res) => {
      toast.success('Rubro agregado')
      queryClient.invalidateQueries({
        queryKey: ['get', '/api/budget-versions/{versionId}/items', { params: { path: { versionId: selectedVersionId! } } }]
      })
      setSelectedBudgetItemId(res.id!)
      setShowItemModal(false)
      itemForm.reset()
    },
    onError: (err) => toast.error(apiErrorMessage(err, 'Error al agregar rubro')),
  })

  const deleteItemMutation = api.useMutation('delete', '/api/budget-items/{id}', {
    onSuccess: () => {
      toast.success('Rubro eliminado del presupuesto')
      queryClient.invalidateQueries({
        queryKey: ['get', '/api/budget-versions/{versionId}/items', { params: { path: { versionId: selectedVersionId! } } }]
      })
      setSelectedBudgetItemId(null)
    },
    onError: (err) => toast.error(apiErrorMessage(err, 'Error al eliminar rubro')),
  })

  const updateItemQtyMutation = api.useMutation('put', '/api/budget-items/{id}', {
    onSuccess: () => {
      toast.success('Cantidad actualizada')
      queryClient.invalidateQueries({
        queryKey: ['get', '/api/budget-versions/{versionId}/items', { params: { path: { versionId: selectedVersionId! } } }]
      })
    },
    onError: (err) => toast.error(apiErrorMessage(err, 'Error al actualizar cantidad')),
  })

  // Measurements mutations
  const addMeasurementMutation = api.useMutation('post', '/api/budget-items/{id}/measurements', {
    onSuccess: () => {
      toast.success('Línea de medición agregada')
      queryClient.invalidateQueries({
        queryKey: ['get', '/api/budget-items/{id}/measurements', { params: { path: { id: selectedBudgetItemId! } } }]
      })
      queryClient.invalidateQueries({
        queryKey: ['get', '/api/budget-versions/{versionId}/items', { params: { path: { versionId: selectedVersionId! } } }]
      })
      measurementForm.reset()
    },
    onError: (err) => toast.error(apiErrorMessage(err, 'Error al agregar medición')),
  })

  const deleteMeasurementMutation = api.useMutation('delete', '/api/measurements/{id}', {
    onSuccess: () => {
      toast.success('Línea de medición eliminada')
      queryClient.invalidateQueries({
        queryKey: ['get', '/api/budget-items/{id}/measurements', { params: { path: { id: selectedBudgetItemId! } } }]
      })
      queryClient.invalidateQueries({
        queryKey: ['get', '/api/budget-versions/{versionId}/items', { params: { path: { versionId: selectedVersionId! } } }]
      })
    },
    onError: (err) => toast.error(apiErrorMessage(err, 'Error al eliminar medición')),
  })

  // APU Snapshot mutations
  const updateApuYieldMutation = api.useMutation('put', '/api/budget-items/{id}/apu', {
    onSuccess: () => {
      toast.success('Rendimiento del APU actualizado')
      queryClient.invalidateQueries({
        queryKey: ['get', '/api/budget-items/{id}/apu', { params: { path: { id: selectedBudgetItemId! } } }]
      })
      queryClient.invalidateQueries({
        queryKey: ['get', '/api/item-apus/{apuId}/components', { params: { path: { apuId: selectedItemApuId! } } }]
      })
    },
    onError: (err) => toast.error(apiErrorMessage(err, 'Error al actualizar rendimiento')),
  })

  const addApuCompMutation = api.useMutation('post', '/api/item-apus/{apuId}/components', {
    onSuccess: () => {
      toast.success('Componente de insumo agregado')
      queryClient.invalidateQueries({
        queryKey: ['get', '/api/item-apus/{apuId}/components', { params: { path: { apuId: selectedItemApuId! } } }]
      })
      setShowApuCompModal(false)
      apuCompForm.reset()
    },
    onError: (err) => toast.error(apiErrorMessage(err, 'Error al agregar componente')),
  })

  const deleteApuCompMutation = api.useMutation('delete', '/api/item-apu-components/{id}', {
    onSuccess: () => {
      toast.success('Componente eliminado')
      queryClient.invalidateQueries({
        queryKey: ['get', '/api/item-apus/{apuId}/components', { params: { path: { apuId: selectedItemApuId! } } }]
      })
    },
    onError: (err) => toast.error(apiErrorMessage(err, 'Error al eliminar componente')),
  })

  // Forms
  const chapterForm = useForm({
    resolver: zodResolver(chapterSchema) as unknown as Resolver<ChapterFormValues>,
    defaultValues: { name: '', parentId: '' }
  })

  const itemForm = useForm({
    resolver: zodResolver(itemSchema) as unknown as Resolver<ItemFormValues>,
    defaultValues: { rubroId: '', quantity: 1 }
  })

  const measurementForm = useForm({
    resolver: zodResolver(measurementSchema) as unknown as Resolver<MeasurementFormValues>,
    defaultValues: { description: '', length: 1, width: 1, height: 1, itemCount: 1, factor: 1, formula: '', notes: '' }
  })

  const apuYieldForm = useForm({
    resolver: zodResolver(apuYieldSchema) as unknown as Resolver<zod.infer<typeof apuYieldSchema>>,
    defaultValues: { yield: 1 }
  })

  const apuCompForm = useForm({
    resolver: zodResolver(apuCompSchema) as unknown as Resolver<zod.infer<typeof apuCompSchema>>,
    defaultValues: { section: 'MATERIALES' as const, insumoId: '', description: '', unitId: '', quantity: 1, yield: 1, wasteFactor: 0, unitPrice: 0 }
  })

  function handleCreateChapter(values: ChapterFormValues) {
    if (!selectedVersionId) return
    createChapterMutation.mutate({
      params: { path: { versionId: selectedVersionId } },
      body: { name: values.name, ...(values.parentId ? { parentId: values.parentId } : {}) }
    })
  }

  function handleAddItem(values: ItemFormValues) {
    if (!selectedChapterIdForAddingItem || !selectedVersionId) return
    addItemMutation.mutate({
      params: { path: { versionId: selectedVersionId } },
      body: { rubroId: values.rubroId, chapterId: selectedChapterIdForAddingItem, quantity: values.quantity }
    })
  }

  function handleAddMeasurement(values: MeasurementFormValues) {
    if (!selectedBudgetItemId) return
    addMeasurementMutation.mutate({
      params: { path: { id: selectedBudgetItemId } },
      body: {
        description: values.description,
        ...(values.length ? { length: values.length } : {}),
        ...(values.width ? { width: values.width } : {}),
        ...(values.height ? { height: values.height } : {}),
        ...(values.itemCount ? { itemCount: values.itemCount } : {}),
        ...(values.factor ? { factor: values.factor } : {}),
        ...(values.formula ? { formula: values.formula } : {}),
        ...(values.notes ? { notes: values.notes } : {}),
      }
    })
  }

  const activeVersion = versionQuery.data

  return (
    <div className="space-y-4 animate-fade-in">
      {/* Top financial header */}
      {activeVersion && (
        <div className="rounded-xl border border-border bg-panel p-4 flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
          <div>
            <div className="text-xs text-muted-foreground font-semibold">Presupuesto en Edición</div>
            <h2 className="text-lg font-bold text-foreground">{activeVersion.name}</h2>
          </div>
          <div className="flex gap-6 text-sm text-foreground">
            <div>
              <span className="text-muted-foreground block text-[10px] uppercase font-bold">Costo Directo</span>
              <span className="font-bold">$ {activeVersion.totalCost?.toLocaleString(undefined, { minimumFractionDigits: 2 })}</span>
            </div>
            <div>
              <span className="text-muted-foreground block text-[10px] uppercase font-bold">Precio Venta</span>
              <span className="font-bold text-primary">$ {activeVersion.salePrice?.toLocaleString(undefined, { minimumFractionDigits: 2 })}</span>
            </div>
            <div>
              <span className="text-muted-foreground block text-[10px] uppercase font-bold">Viabilidad</span>
              <div className="mt-0.5">
                <StatusBadge tone={viabilityTone(activeVersion.viabilityStatus)}>
                  {activeVersion.viabilityStatus || 'Borrador'}
                </StatusBadge>
              </div>
            </div>
          </div>
          {activeVersion.status !== 'APROBADO' && (
            <div className="flex gap-2">
              <Button size="sm" onClick={() => setShowChapterModal(true)}>
                <FolderPlus className="size-4" />
                Crear Capítulo
              </Button>
            </div>
          )}
        </div>
      )}

      {selectedVersionId ? (
        <div className="grid gap-4 lg:grid-cols-[1fr_380px] items-start">
          {/* Left panel: hierarchical chapters & rubros */}
          <div className="rounded-xl border border-border bg-panel p-4 space-y-4">
            <h3 className="font-bold text-sm border-b border-border pb-2 flex items-center gap-2">
              <Settings className="size-4 text-primary" />
              Estructura de Capítulos y Rubros
            </h3>

            <div className="space-y-4 max-h-[500px] overflow-y-auto pr-2">
              {chaptersQuery.data?.map(chapter => {
                const chapterItems = itemsQuery.data?.filter(i => i.chapterId === chapter.id) || []
                return (
                  <div key={chapter.id} className="border border-border/60 rounded-xl overflow-hidden bg-background">
                    {/* Chapter Header */}
                    <div className="bg-secondary/40 px-3 py-2 flex justify-between items-center border-b border-border">
                      <div className="flex items-center gap-2">
                        <span className="text-xs font-mono font-bold text-primary bg-primary/10 px-1.5 py-0.5 rounded">
                          {chapter.code}
                        </span>
                        <h4 className="font-bold text-sm text-foreground">{chapter.name}</h4>
                      </div>
                      {activeVersion?.status !== 'APROBADO' && (
                        <Button
                          size="sm"
                          variant="ghost"
                          onClick={() => {
                            setSelectedChapterIdForAddingItem(chapter.id!)
                            setShowItemModal(true)
                          }}
                          className="text-xs h-7 gap-1"
                        >
                          <Plus className="size-3" />
                          Añadir Rubro
                        </Button>
                      )}
                    </div>

                    {/* Chapter Items List */}
                    <div className="divide-y divide-border">
                      {chapterItems.map(item => (
                        <div
                          key={item.id}
                          onClick={() => setSelectedBudgetItemId(item.id!)}
                          className={`flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 p-3 cursor-pointer transition-colors ${
                            selectedBudgetItemId === item.id ? 'bg-primary/5 border-l-2 border-primary' : 'hover:bg-accent/25'
                          }`}
                        >
                          <div className="space-y-0.5 flex-1">
                            <div className="flex items-center gap-2">
                              <span className="text-[10px] font-mono text-muted-foreground bg-secondary px-1.5 py-0.5 rounded">
                                {item.code}
                              </span>
                              <h5 className="font-semibold text-xs text-foreground">{item.name}</h5>
                            </div>
                            <span className="text-[10px] text-muted-foreground block">
                              Unidad: {unitsQuery.data?.find(u => u.id === item.unitId)?.code || '-'}
                            </span>
                          </div>

                          <div className="flex items-center gap-4 w-full sm:w-auto justify-between sm:justify-end">
                            <div className="w-24">
                              {activeVersion?.status !== 'APROBADO' ? (
                                <Input
                                  type="number"
                                  step="0.0001"
                                  defaultValue={item.quantity || 0}
                                  onBlur={(e) => {
                                    const val = parseFloat(e.target.value)
                                    if (!isNaN(val) && val !== item.quantity) {
                                      updateItemQtyMutation.mutate({
                                        params: { path: { id: item.id! } },
                                        body: { quantity: val }
                                      })
                                    }
                                  }}
                                  className="h-8 text-xs font-semibold"
                                  aria-label={`Cantidad para ${item.name}`}
                                />
                              ) : (
                                <span className="font-bold text-xs">{item.quantity}</span>
                              )}
                            </div>

                            <div className="text-right min-w-24">
                              <p className="text-[10px] text-muted-foreground">Venta Unit.</p>
                              <p className="font-bold text-xs text-foreground">
                                $ {item.unitPrice?.toFixed(2) || '0.00'}
                              </p>
                            </div>

                            <div className="text-right min-w-24">
                              <p className="text-[10px] text-muted-foreground">Total Venta</p>
                              <p className="font-bold text-xs text-primary">
                                $ {item.totalSale?.toFixed(2) || '0.00'}
                              </p>
                            </div>

                            {activeVersion?.status !== 'APROBADO' && (
                              <span onClick={(e) => e.stopPropagation()}>
                                <ConfirmAction
                                  triggerLabel={`Eliminar rubro ${item.name ?? ''}`}
                                  message="¿Eliminar este rubro de la versión?"
                                  confirmLabel="Eliminar"
                                  destructive
                                  disabled={deleteItemMutation.isPending}
                                  onConfirm={() => deleteItemMutation.mutate({ params: { path: { id: item.id! } } })}
                                >
                                  <Trash2 className="size-4" />
                                </ConfirmAction>
                              </span>
                            )}
                          </div>
                        </div>
                      ))}
                      {chapterItems.length === 0 && (
                        <p className="text-center text-xs text-muted-foreground py-4">No hay rubros en este capítulo.</p>
                      )}
                    </div>
                  </div>
                )
              })}
              {(!chaptersQuery.data || chaptersQuery.data.length === 0) && (
                <div className="py-12 text-center text-muted-foreground border border-dashed border-border rounded-xl">
                  Aún no se han configurado capítulos en esta versión. Haz clic en "Crear Capítulo" para comenzar.
                </div>
              )}
            </div>
          </div>

          {/* Right panel: details (Mediciones & APU Snapshot) */}
          <div>
            {selectedBudgetItemId && selectedItem ? (
              <div className="rounded-xl border border-border bg-panel p-4 space-y-4 animate-fade-in">
                {/* Tab selectors */}
                <div className="flex bg-secondary p-0.5 rounded-lg border border-border">
                  <Button
                    variant={rightPanelTab === 'measurements' ? 'default' : 'ghost'}
                    size="sm"
                    className="flex-1 text-xs"
                    onClick={() => setRightPanelTab('measurements')}
                  >
                    <Ruler className="size-3.5" />
                    Mediciones
                  </Button>
                  <Button
                    variant={rightPanelTab === 'apu' ? 'default' : 'ghost'}
                    size="sm"
                    className="flex-1 text-xs"
                    onClick={() => setRightPanelTab('apu')}
                  >
                    <Settings className="size-3.5" />
                    APU
                  </Button>
                </div>

                <div className="border-b border-border pb-2">
                  <span className="text-[10px] text-muted-foreground font-mono">{selectedItem.code}</span>
                  <h4 className="font-bold text-sm text-foreground truncate">{selectedItem.name}</h4>
                </div>

                {/* MEASUREMENTS TAB */}
                {rightPanelTab === 'measurements' && (
                  <div className="space-y-4">
                    {/* Add measurement line */}
                    {activeVersion?.status !== 'APROBADO' && (
                      <form onSubmit={measurementForm.handleSubmit(handleAddMeasurement)} className="border border-border/60 bg-background p-3 rounded-lg space-y-2">
                        <Input placeholder="Descripción línea (Ej. Longitud eje A)" {...measurementForm.register('description')} className="h-8 text-xs" />
                        <div className="grid grid-cols-5 gap-1.5">
                          <Input type="number" step="0.01" placeholder="Largo" {...measurementForm.register('length')} className="h-8 text-xs px-1 text-center" title="Largo" />
                          <Input type="number" step="0.01" placeholder="Ancho" {...measurementForm.register('width')} className="h-8 text-xs px-1 text-center" title="Ancho" />
                          <Input type="number" step="0.01" placeholder="Alto" {...measurementForm.register('height')} className="h-8 text-xs px-1 text-center" title="Alto" />
                          <Input type="number" step="0.01" placeholder="Cant" {...measurementForm.register('itemCount')} className="h-8 text-xs px-1 text-center" title="Cantidad de Elementos" />
                          <Input type="number" step="0.01" placeholder="Fact" {...measurementForm.register('factor')} className="h-8 text-xs px-1 text-center" title="Factor" />
                        </div>
                        <div className="flex gap-2">
                          <Input placeholder="Fórmula" {...measurementForm.register('formula')} className="h-8 text-xs flex-1" title="Fórmula de cálculo (ej. L*A*H*C*F)" />
                          <Button type="submit" size="sm" className="h-8 px-2 text-xs" disabled={addMeasurementMutation.isPending}>
                            Agregar
                          </Button>
                        </div>
                      </form>
                    )}

                    {/* Measurements List */}
                    {measurementsQuery.isLoading ? (
                      <div className="h-8 w-full animate-pulse bg-muted rounded-md" />
                    ) : (
                      <div className="space-y-2 max-h-64 overflow-y-auto">
                        {measurementsQuery.data?.map(m => (
                          <div key={m.id} className="flex justify-between items-center border border-border bg-background p-2 rounded-lg text-xs hover:bg-accent/20">
                            <div>
                              <p className="font-semibold text-foreground">{m.description}</p>
                              <span className="text-[10px] text-muted-foreground">
                                {m.itemCount}x({m.length}x{m.width}x{m.height}) * f:{m.factor}
                              </span>
                            </div>
                            <div className="flex items-center gap-2">
                              <span className="font-bold text-primary font-mono">{m.result?.toFixed(4)}</span>
                              {activeVersion?.status !== 'APROBADO' && (
                                <ConfirmAction
                                  triggerLabel="Eliminar medición"
                                  message="¿Eliminar esta medición?"
                                  confirmLabel="Eliminar"
                                  destructive
                                  disabled={deleteMeasurementMutation.isPending}
                                  onConfirm={() => deleteMeasurementMutation.mutate({ params: { path: { id: m.id! } } })}
                                >
                                  <Trash2 className="size-3.5" />
                                </ConfirmAction>
                              )}
                            </div>
                          </div>
                        ))}
                        {(!measurementsQuery.data || measurementsQuery.data.length === 0) && (
                          <p className="text-center text-xs text-muted-foreground py-4">No hay mediciones registradas.</p>
                        )}
                      </div>
                    )}
                  </div>
                )}

                {/* APU SNAPSHOT TAB */}
                {rightPanelTab === 'apu' && (
                  <div className="space-y-4">
                    {/* APU yield edit */}
                    <div className="flex justify-between items-center border-b border-border pb-2 text-xs">
                      <span className="text-muted-foreground">APU congelado en versión</span>
                      {activeVersion?.status !== 'APROBADO' ? (
                        <form
                          onSubmit={apuYieldForm.handleSubmit((v) => updateApuYieldMutation.mutate({ params: { path: { id: selectedBudgetItemId! } }, body: { yield: v.yield } }))}
                          className="flex gap-2 items-center"
                        >
                          <label className="text-[10px] text-muted-foreground">Rendimiento:</label>
                          <Input
                            type="number"
                            step="0.0001"
                            {...apuYieldForm.register('yield')}
                            className="w-16 h-7 text-xs px-1 text-center"
                          />
                          <Button type="submit" size="sm" className="h-7 px-1.5">Ok</Button>
                        </form>
                      ) : (
                        <span>Rendimiento: <strong>{apuQuery.data?.yield || 1}</strong></span>
                      )}
                    </div>

                    <div className="flex justify-between items-center">
                      <h4 className="text-xs font-bold">Insumos Componentes</h4>
                      {activeVersion?.status !== 'APROBADO' && selectedItemApuId && (
                        <Button size="sm" variant="outline" className="h-7 text-xs" onClick={() => setShowApuCompModal(true)}>
                          + Componente
                        </Button>
                      )}
                    </div>

                    {/* Components List */}
                    {apuComponentsQuery.isLoading ? (
                      <div className="h-8 w-full animate-pulse bg-muted rounded-md" />
                    ) : (
                      <div className="space-y-1.5 max-h-64 overflow-y-auto">
                        {apuComponentsQuery.data?.map(comp => (
                          <div key={comp.id} className="flex justify-between items-center border border-border/80 bg-background p-2 rounded-lg text-xs">
                            <div className="space-y-0.5 max-w-[200px]">
                              <p className="font-semibold truncate text-[11px]">{comp.description}</p>
                              <span className="text-[9px] text-muted-foreground uppercase">{comp.section} | Cant: {comp.quantity}</span>
                            </div>
                            <div className="flex items-center gap-2">
                              <span className="font-bold text-foreground font-mono">${comp.lineTotal?.toFixed(2)}</span>
                              {activeVersion?.status !== 'APROBADO' && (
                                <ConfirmAction
                                  triggerLabel="Eliminar componente de APU"
                                  message="¿Eliminar este componente?"
                                  confirmLabel="Eliminar"
                                  destructive
                                  disabled={deleteApuCompMutation.isPending}
                                  onConfirm={() => deleteApuCompMutation.mutate({ params: { path: { id: comp.id! } } })}
                                >
                                  <Trash2 className="size-3.5" />
                                </ConfirmAction>
                              )}
                            </div>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                )}
              </div>
            ) : (
              <div className="rounded-xl border border-dashed border-border bg-panel/30 p-8 text-center text-muted-foreground flex flex-col items-center justify-center h-48">
                <Sparkles className="size-8 text-muted-foreground/45 mb-2" />
                Haz clic en cualquier rubro del presupuesto a la izquierda para inspeccionar sus mediciones y APU congelado.
              </div>
            )}
          </div>
        </div>
      ) : (
        <div className="rounded-xl border border-dashed border-border bg-panel/30 p-12 text-center text-muted-foreground h-64 flex flex-col items-center justify-center">
          <ShieldAlert className="size-8 text-muted-foreground/45 mb-2" />
          No se ha seleccionado ninguna versión de presupuesto. Por favor, selecciona una versión en la pestaña Presupuestos.
        </div>
      )}

      {/* Chapter Modal */}
      {showChapterModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs">
          <div className="w-full max-w-md bg-panel border border-border p-6 rounded-xl shadow-lg space-y-4 animate-scale-up">
            <h2 className="text-lg font-bold border-b border-border pb-2">Crear Capítulo / Subcapítulo</h2>
            <form onSubmit={chapterForm.handleSubmit(handleCreateChapter)} className="space-y-4">
              <div>
                <label className="text-xs font-semibold text-muted-foreground mb-1 block">Nombre del Capítulo</label>
                <Input placeholder="Ej. Obras Preliminares" {...chapterForm.register('name')} />
                {chapterForm.formState.errors.name && (
                  <p className="mt-1 text-xs text-destructive">{chapterForm.formState.errors.name.message}</p>
                )}
              </div>
              <div>
                <label className="text-xs font-semibold text-muted-foreground mb-1 block">Capítulo Padre (Jerarquía)</label>
                <select
                  className="flex h-9 w-full rounded-md border border-border bg-background px-3 py-1 text-sm shadow-xs focus-visible:outline-hidden"
                  {...chapterForm.register('parentId')}
                >
                  <option value="">Raíz (Sin padre)...</option>
                  {chaptersQuery.data?.map(c => (
                    <option key={c.id} value={c.id}>[{c.code}] {c.name}</option>
                  ))}
                </select>
              </div>

              <div className="flex justify-end gap-2 border-t border-border pt-3">
                <Button type="button" variant="outline" onClick={() => setShowChapterModal(false)}>
                  Cancelar
                </Button>
                <Button type="submit" disabled={createChapterMutation.isPending}>
                  Crear Capítulo
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Item Modal (Add Rubro to Chapter) */}
      {showItemModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs overflow-y-auto">
          <div className="w-full max-w-md bg-panel border border-border p-6 rounded-xl shadow-lg my-8 space-y-4 animate-scale-up">
            <h2 className="text-lg font-bold border-b border-border pb-2">Añadir Rubro al Capítulo</h2>
            <form onSubmit={itemForm.handleSubmit(handleAddItem)} className="space-y-4">
              <div>
                <label className="text-xs font-semibold text-muted-foreground mb-1 block">Seleccionar Rubro del Catálogo</label>
                <select
                  className="flex h-9 w-full rounded-md border border-border bg-background px-3 py-1 text-sm shadow-xs focus-visible:outline-hidden"
                  {...itemForm.register('rubroId')}
                >
                  <option value="">Seleccione rubro...</option>
                  {rubrosCatalogQuery.data?.filter(r => r.status === 'ACTIVO').map(r => (
                    <option key={r.id} value={r.id}>[{r.code}] {r.name}</option>
                  ))}
                </select>
                {itemForm.formState.errors.rubroId && (
                  <p className="mt-1 text-xs text-destructive">{itemForm.formState.errors.rubroId.message}</p>
                )}
              </div>
              <div>
                <label className="text-xs font-semibold text-muted-foreground mb-1 block">Cantidad Estimada inicial</label>
                <Input type="number" step="0.0001" {...itemForm.register('quantity')} />
                {itemForm.formState.errors.quantity && (
                  <p className="mt-1 text-xs text-destructive">{itemForm.formState.errors.quantity.message}</p>
                )}
              </div>

              <div className="flex justify-end gap-2 border-t border-border pt-3">
                <Button type="button" variant="outline" onClick={() => setShowItemModal(false)}>
                  Cancelar
                </Button>
                <Button type="submit" disabled={addItemMutation.isPending}>
                  Añadir Rubro
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* APU Component Modal */}
      {showApuCompModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs overflow-y-auto">
          <div className="w-full max-w-md bg-panel border border-border p-6 rounded-xl shadow-lg my-8 space-y-4 animate-scale-up">
            <h2 className="text-lg font-bold border-b border-border pb-2">Agregar Insumo al APU Congelado</h2>
            <form
              onSubmit={apuCompForm.handleSubmit((v) => {
                const ins = insumosCatalogQuery.data?.find(i => i.id === v.insumoId)
                const desc = v.description || ins?.name
                const unitPrice = v.unitPrice || ins?.referencePrice
                addApuCompMutation.mutate({
                  params: { path: { apuId: selectedItemApuId! } },
                  body: {
                    section: v.section,
                    unitId: v.unitId || ins?.unitId || '',
                    quantity: v.quantity,
                    ...(v.insumoId ? { insumoId: v.insumoId } : {}),
                    ...(desc ? { description: desc } : {}),
                    ...(typeof v.yield === 'number' ? { yield: v.yield } : {}),
                    ...(typeof v.wasteFactor === 'number' ? { wasteFactor: v.wasteFactor } : {}),
                    ...(typeof unitPrice === 'number' ? { unitPrice } : {}),
                  },
                })
              })}
              className="space-y-4"
            >
              <div>
                <label className="text-xs font-semibold text-muted-foreground mb-1 block">Sección</label>
                <select
                  className="flex h-9 w-full rounded-md border border-border bg-background px-3 py-1 text-sm shadow-xs focus-visible:outline-hidden"
                  {...apuCompForm.register('section')}
                >
                  <option value="MATERIALES">Materiales</option>
                  <option value="MANO_DE_OBRA">Mano de Obra</option>
                  <option value="EQUIPOS">Equipos / Herramientas</option>
                  <option value="TRANSPORTE">Transporte</option>
                  <option value="SUBCONTRATOS">Subcontratos</option>
                  <option value="OTROS">Otros</option>
                </select>
              </div>

              <div>
                <label className="text-xs font-semibold text-muted-foreground mb-1 block">Insumo del Catálogo</label>
                <select
                  className="flex h-9 w-full rounded-md border border-border bg-background px-3 py-1 text-sm shadow-xs focus-visible:outline-hidden"
                  {...apuCompForm.register('insumoId')}
                  onChange={(e) => {
                    const ins = insumosCatalogQuery.data?.find(i => i.id === e.target.value)
                    if (ins) {
                      apuCompForm.setValue('unitId', ins.unitId!)
                      apuCompForm.setValue('unitPrice', ins.referencePrice || 0)
                      apuCompForm.setValue('description', ins.name!)
                    }
                  }}
                >
                  <option value="">Seleccione insumo...</option>
                  {insumosCatalogQuery.data?.map(i => (
                    <option key={i.id} value={i.id}>[{i.code}] {i.name} - ${i.referencePrice?.toFixed(2)}</option>
                  ))}
                </select>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="text-xs font-semibold text-muted-foreground mb-1 block">Cantidad</label>
                  <Input type="number" step="0.0001" {...apuCompForm.register('quantity')} />
                </div>
                <div>
                  <label className="text-xs font-semibold text-muted-foreground mb-1 block">Unidad</label>
                  <select
                    className="flex h-9 w-full rounded-md border border-border bg-background px-3 py-1 text-sm shadow-xs focus-visible:outline-hidden"
                    {...apuCompForm.register('unitId')}
                  >
                    {unitsQuery.data?.map(u => (
                      <option key={u.id} value={u.id}>{u.code}</option>
                    ))}
                  </select>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="text-xs font-semibold text-muted-foreground mb-1 block">Rendimiento (Mano Obra/Equipos)</label>
                  <Input type="number" step="0.0001" {...apuCompForm.register('yield')} />
                </div>
                <div>
                  <label className="text-xs font-semibold text-muted-foreground mb-1 block">Desperdicio (Factor 0..1)</label>
                  <Input type="number" step="0.000001" {...apuCompForm.register('wasteFactor')} />
                </div>
              </div>

              <div>
                <label className="text-xs font-semibold text-muted-foreground mb-1 block">Precio Unitario ($)</label>
                <Input type="number" step="0.0001" {...apuCompForm.register('unitPrice')} />
              </div>

              <div className="flex justify-end gap-2 border-t border-border pt-3">
                <Button type="button" variant="outline" onClick={() => setShowApuCompModal(false)}>
                  Cancelar
                </Button>
                <Button type="submit" disabled={addApuCompMutation.isPending}>
                  Agregar
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}
