import { useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm, type Resolver } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import * as zod from 'zod'
import { toast } from 'sonner'
import { FileText, Hammer, Package, Percent, Settings, Plus, Trash2, Settings2 } from 'lucide-react'

import { api } from '@/lib/api/client'
import { apiErrorMessage } from '@/lib/api/errors'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'

// Schema definitions
const unitSchema = zod.object({
  code: zod.string().min(1).max(40),
  name: zod.string().min(2).max(120),
})
type UnitFormValues = zod.infer<typeof unitSchema>

const categorySchema = zod.object({
  code: zod.string().min(1).max(40),
  name: zod.string().min(2).max(160),
})
type CategoryFormValues = zod.infer<typeof categorySchema>

const taxSchema = zod.object({
  code: zod.string().min(1).max(40),
  name: zod.string().min(2).max(120),
  rate: zod.coerce.number().min(0).max(100),
})
type TaxFormValues = zod.infer<typeof taxSchema>

const insumoSchema = zod.object({
  code: zod.string().min(2, 'Mínimo 2 caracteres').max(60),
  name: zod.string().min(3, 'Mínimo 3 caracteres').max(200),
  description: zod.string().optional(),
  unitId: zod.string().uuid('Seleccione unidad'),
  type: zod.enum(['MATERIAL', 'MANO_DE_OBRA', 'EQUIPO', 'TRANSPORTE', 'SUBCONTRATO', 'OTRO']),
  categoryId: zod.string().uuid('Seleccione categoría').optional().or(zod.string().length(0)),
  brand: zod.string().optional(),
  specification: zod.string().optional(),
  referencePrice: zod.coerce.number().min(0).default(0),
  referencePriceCurrency: zod.string().length(3).default('USD'),
})
type InsumoFormValues = zod.infer<typeof insumoSchema>

const apuSchema = zod.object({
  code: zod.string().min(2).max(60),
  name: zod.string().min(3).max(200),
  unitId: zod.string().uuid('Seleccione unidad'),
  yield: zod.coerce.number().min(0.0001, 'Rendimiento debe ser mayor a 0').default(1),
  validUntil: zod.string().optional(),
})
type ApuFormValues = zod.infer<typeof apuSchema>

const apuComponentSchema = zod.object({
  section: zod.enum(['MATERIALES', 'MANO_DE_OBRA', 'EQUIPOS', 'TRANSPORTE', 'SUBCONTRATOS', 'OTROS']),
  insumoId: zod.string().uuid('Seleccione insumo').optional().or(zod.string().length(0)),
  description: zod.string().optional(),
  unitId: zod.string().uuid('Seleccione unidad'),
  quantity: zod.coerce.number().min(0, 'Mínimo 0'),
  yield: zod.coerce.number().min(0).optional(),
  wasteFactor: zod.coerce.number().min(0).default(0),
  unitPrice: zod.coerce.number().min(0).default(0),
})
type ApuComponentFormValues = zod.infer<typeof apuComponentSchema>

const rubroSchema = zod.object({
  code: zod.string().min(2).max(60),
  name: zod.string().min(3).max(200),
  description: zod.string().optional(),
  unitId: zod.string().uuid('Seleccione unidad'),
  categoryId: zod.string().uuid('Seleccione categoría').optional().or(zod.string().length(0)),
  specification: zod.string().optional(),
  keywords: zod.string().optional(),
  baseApuId: zod.string().uuid('Seleccione APU').optional().or(zod.string().length(0)),
})
type RubroFormValues = zod.infer<typeof rubroSchema>

export function CatalogTab() {
  const queryClient = useQueryClient()
  const [activeSubTab, setActiveSubTab] = useState<'insumos' | 'rubros' | 'apus' | 'config'>('insumos')
  const [selectedApuId, setSelectedApuId] = useState<string | null>(null)
  
  // Modals
  const [showInsumoModal, setShowInsumoModal] = useState(false)
  const [showApuModal, setShowApuModal] = useState(false)
  const [showComponentModal, setShowComponentModal] = useState(false)
  const [showRubroModal, setShowRubroModal] = useState(false)

  // Queries
  const unitsQuery = useQuery(api.queryOptions('get', '/api/admin/units'))
  const categoriesQuery = useQuery(api.queryOptions('get', '/api/admin/categories'))
  const taxesQuery = useQuery(api.queryOptions('get', '/api/admin/taxes'))
  const insumosQuery = useQuery(api.queryOptions('get', '/api/insumos'))
  const apusesQuery = useQuery(api.queryOptions('get', '/api/apuses'))
  const rubrosQuery = useQuery(api.queryOptions('get', '/api/rubros'))
  
  const apuComponentsQuery = useQuery({
    ...api.queryOptions('get', '/api/apuses/{apuId}/components', { params: { path: { apuId: selectedApuId || '' } } }),
    enabled: !!selectedApuId,
  })

  // Mutations Configuration
  const createUnitMutation = api.useMutation('post', '/api/admin/units', {
    onSuccess: () => {
      toast.success('Unidad creada')
      queryClient.invalidateQueries({ queryKey: ['get', '/api/admin/units'] })
      unitForm.reset()
    },
    onError: (err) => toast.error(apiErrorMessage(err, 'Error al crear unidad')),
  })

  const createCategoryMutation = api.useMutation('post', '/api/admin/categories', {
    onSuccess: () => {
      toast.success('Categoría creada')
      queryClient.invalidateQueries({ queryKey: ['get', '/api/admin/categories'] })
      categoryForm.reset()
    },
    onError: (err) => toast.error(apiErrorMessage(err, 'Error al crear categoría')),
  })

  const createTaxMutation = api.useMutation('post', '/api/admin/taxes', {
    onSuccess: () => {
      toast.success('Impuesto creado')
      queryClient.invalidateQueries({ queryKey: ['get', '/api/admin/taxes'] })
      taxForm.reset()
    },
    onError: (err) => toast.error(apiErrorMessage(err, 'Error al crear impuesto')),
  })

  // Mutations Insumos
  const createInsumoMutation = api.useMutation('post', '/api/insumos', {
    onSuccess: () => {
      toast.success('Insumo comercial creado')
      queryClient.invalidateQueries({ queryKey: ['get', '/api/insumos'] })
      setShowInsumoModal(false)
      insumoForm.reset()
    },
    onError: (err) => toast.error(apiErrorMessage(err, 'Error al crear insumo')),
  })

  // Mutations APU
  const createApuMutation = api.useMutation('post', '/api/apuses', {
    onSuccess: (res) => {
      toast.success('APU creado')
      queryClient.invalidateQueries({ queryKey: ['get', '/api/apuses'] })
      setShowApuModal(false)
      setSelectedApuId(res.id!)
      apuForm.reset()
    },
    onError: (err) => toast.error(apiErrorMessage(err, 'Error al crear APU')),
  })

  const addComponentMutation = api.useMutation('post', '/api/apuses/{apuId}/components', {
    onSuccess: () => {
      toast.success('Componente agregado al APU')
      queryClient.invalidateQueries({ queryKey: ['get', '/api/apuses/{apuId}/components', { params: { path: { apuId: selectedApuId! } } }] })
      queryClient.invalidateQueries({ queryKey: ['get', '/api/apuses'] })
      setShowComponentModal(false)
      componentForm.reset()
    },
    onError: (err) => toast.error(apiErrorMessage(err, 'Error al agregar componente')),
  })

  const removeComponentMutation = api.useMutation('delete', '/api/apuses/{apuId}/components/{componentId}', {
    onSuccess: () => {
      toast.success('Componente removido')
      queryClient.invalidateQueries({ queryKey: ['get', '/api/apuses/{apuId}/components', { params: { path: { apuId: selectedApuId! } } }] })
      queryClient.invalidateQueries({ queryKey: ['get', '/api/apuses'] })
    },
    onError: (err) => toast.error(apiErrorMessage(err, 'Error al remover componente')),
  })

  // Mutations Rubro
  const createRubroMutation = api.useMutation('post', '/api/rubros', {
    onSuccess: () => {
      toast.success('Rubro maestro creado')
      queryClient.invalidateQueries({ queryKey: ['get', '/api/rubros'] })
      setShowRubroModal(false)
      rubroForm.reset()
    },
    onError: (err) => toast.error(apiErrorMessage(err, 'Error al crear rubro')),
  })

  // Forms hook-form
  const unitForm = useForm({ resolver: zodResolver(unitSchema) as unknown as Resolver<UnitFormValues>, defaultValues: { code: '', name: '' } })
  const categoryForm = useForm({ resolver: zodResolver(categorySchema) as unknown as Resolver<CategoryFormValues>, defaultValues: { code: '', name: '' } })
  const taxForm = useForm({ resolver: zodResolver(taxSchema) as unknown as Resolver<TaxFormValues>, defaultValues: { code: '', name: '', rate: 0 } })
  
  const insumoForm = useForm({
    resolver: zodResolver(insumoSchema) as unknown as Resolver<InsumoFormValues>,
    defaultValues: {
      code: '', name: '', description: '', unitId: '', type: 'MATERIAL' as const,
      categoryId: '', brand: '', specification: '', referencePrice: 0, referencePriceCurrency: 'USD'
    }
  })

  const apuForm = useForm({
    resolver: zodResolver(apuSchema) as unknown as Resolver<ApuFormValues>,
    defaultValues: { code: '', name: '', unitId: '', yield: 1, validUntil: '' }
  })

  const componentForm = useForm({
    resolver: zodResolver(apuComponentSchema) as unknown as Resolver<ApuComponentFormValues>,
    defaultValues: { section: 'MATERIALES' as const, insumoId: '', description: '', unitId: '', quantity: 1, yield: 1, wasteFactor: 0, unitPrice: 0 }
  })

  const rubroForm = useForm({
    resolver: zodResolver(rubroSchema) as unknown as Resolver<RubroFormValues>,
    defaultValues: { code: '', name: '', description: '', unitId: '', categoryId: '', specification: '', keywords: '', baseApuId: '' }
  })

  function handleCreateUnit(values: UnitFormValues) {
    createUnitMutation.mutate({ body: values })
  }

  function handleCreateCategory(values: CategoryFormValues) {
    createCategoryMutation.mutate({ body: values })
  }

  function handleCreateTax(values: TaxFormValues) {
    createTaxMutation.mutate({ body: { code: values.code, name: values.name, rate: values.rate / 100 } })
  }

  function handleCreateInsumo(values: InsumoFormValues) {
    createInsumoMutation.mutate({
      body: {
        code: values.code,
        name: values.name,
        unitId: values.unitId,
        type: values.type,
        referencePrice: values.referencePrice,
        referencePriceCurrency: values.referencePriceCurrency,
        ...(values.description ? { description: values.description } : {}),
        ...(values.categoryId ? { categoryId: values.categoryId } : {}),
        ...(values.brand ? { brand: values.brand } : {}),
        ...(values.specification ? { specification: values.specification } : {}),
      }
    })
  }

  function handleCreateApu(values: ApuFormValues) {
    createApuMutation.mutate({
      body: {
        code: values.code,
        name: values.name,
        unitId: values.unitId,
        yield: values.yield,
        ...(values.validUntil ? { validUntil: values.validUntil } : {}),
      }
    })
  }

  function handleAddComponent(values: ApuComponentFormValues) {
    if (!selectedApuId) return
    const ins = insumosQuery.data?.find(i => i.id === values.insumoId)
    addComponentMutation.mutate({
      params: { path: { apuId: selectedApuId } },
      body: {
        section: values.section,
        description: values.description || ins?.name || 'Insumo',
        unitId: values.unitId || ins?.unitId || '',
        quantity: values.quantity,
        ...(values.insumoId ? { insumoId: values.insumoId } : {}),
        ...(values.yield ? { yield: values.yield } : {}),
        ...(values.wasteFactor ? { wasteFactor: values.wasteFactor } : {}),
        ...(values.unitPrice ? { unitPrice: values.unitPrice } : ins?.referencePrice ? { unitPrice: ins.referencePrice } : {}),
      }
    })
  }

  function handleCreateRubro(values: RubroFormValues) {
    createRubroMutation.mutate({
      body: {
        code: values.code,
        name: values.name,
        unitId: values.unitId,
        ...(values.description ? { description: values.description } : {}),
        ...(values.categoryId ? { categoryId: values.categoryId } : {}),
        ...(values.specification ? { specification: values.specification } : {}),
        ...(values.keywords ? { keywords: values.keywords } : {}),
        ...(values.baseApuId ? { baseApuId: values.baseApuId } : {}),
      }
    })
  }

  return (
    <div className="space-y-6 animate-fade-in">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 border-b border-border pb-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Catálogo Técnico</h1>
          <p className="text-sm text-muted-foreground">Gestiona insumos, rubros, análisis de precios unitarios (APU) y unidades base.</p>
        </div>
        
        {/* Navigation Tabs */}
        <div className="flex bg-secondary p-0.5 rounded-lg border border-border">
          <Button
            variant={activeSubTab === 'insumos' ? 'default' : 'ghost'}
            size="sm"
            onClick={() => setActiveSubTab('insumos')}
          >
            Insumos
          </Button>
          <Button
            variant={activeSubTab === 'rubros' ? 'default' : 'ghost'}
            size="sm"
            onClick={() => setActiveSubTab('rubros')}
          >
            Rubros
          </Button>
          <Button
            variant={activeSubTab === 'apus' ? 'default' : 'ghost'}
            size="sm"
            onClick={() => setActiveSubTab('apus')}
          >
            APUs
          </Button>
          <Button
            variant={activeSubTab === 'config' ? 'default' : 'ghost'}
            size="sm"
            onClick={() => setActiveSubTab('config')}
          >
            Configuración
          </Button>
        </div>
      </div>

      {/* INSUMOS SUB-TAB */}
      {activeSubTab === 'insumos' && (
        <div className="space-y-4">
          <div className="flex justify-between items-center">
            <h2 className="text-lg font-bold flex items-center gap-2">
              <Package className="size-5 text-primary" />
              Insumos Comerciales Registrados
            </h2>
            <Button size="sm" onClick={() => setShowInsumoModal(true)}>
              <Plus className="size-4" />
              Nuevo Insumo
            </Button>
          </div>

          <div className="overflow-x-auto rounded-xl border border-border bg-panel">
            <table className="w-full text-left text-sm border-collapse">
              <thead>
                <tr className="border-b border-border text-muted-foreground text-xs font-semibold">
                  <th className="p-3">Código</th>
                  <th className="p-3">Nombre</th>
                  <th className="p-3">Tipo</th>
                  <th className="p-3">Unidad</th>
                  <th className="p-3">Precio Ref.</th>
                  <th className="p-3">Estado</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {insumosQuery.data?.map(insumo => (
                  <tr key={insumo.id} className="hover:bg-accent/30 transition-colors">
                    <td className="p-3 font-mono font-semibold text-xs">{insumo.code}</td>
                    <td className="p-3 font-medium">{insumo.name}</td>
                    <td className="p-3 text-xs">{insumo.type}</td>
                    <td className="p-3 text-xs">
                      {unitsQuery.data?.find(u => u.id === insumo.unitId)?.code || 'base'}
                    </td>
                    <td className="p-3 font-medium text-xs">
                      {insumo.referencePriceCurrency} {insumo.referencePrice?.toLocaleString(undefined, { minimumFractionDigits: 4 })}
                    </td>
                    <td className="p-3">
                      <span className={`inline-flex rounded-full px-2 py-0.5 text-xs font-medium ${
                        insumo.status === 'ACTIVO' ? 'bg-emerald-500/10 text-emerald-500' : 'bg-destructive/10 text-destructive'
                      }`}>
                        {insumo.status}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* RUBROS SUB-TAB */}
      {activeSubTab === 'rubros' && (
        <div className="space-y-4">
          <div className="flex justify-between items-center">
            <h2 className="text-lg font-bold flex items-center gap-2">
              <FileText className="size-5 text-primary" />
              Rubros Maestros
            </h2>
            <Button size="sm" onClick={() => setShowRubroModal(true)}>
              <Plus className="size-4" />
              Nuevo Rubro Maestro
            </Button>
          </div>

          <div className="overflow-x-auto rounded-xl border border-border bg-panel">
            <table className="w-full text-left text-sm border-collapse">
              <thead>
                <tr className="border-b border-border text-muted-foreground text-xs font-semibold">
                  <th className="p-3">Código</th>
                  <th className="p-3">Nombre</th>
                  <th className="p-3">Unidad</th>
                  <th className="p-3">Categoría</th>
                  <th className="p-3">APU Asociado</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {rubrosQuery.data?.map(rubro => (
                  <tr key={rubro.id} className="hover:bg-accent/30 transition-colors">
                    <td className="p-3 font-mono font-semibold text-xs">{rubro.code}</td>
                    <td className="p-3 font-medium">{rubro.name}</td>
                    <td className="p-3 text-xs">
                      {unitsQuery.data?.find(u => u.id === rubro.unitId)?.code || '-'}
                    </td>
                    <td className="p-3 text-xs">
                      {categoriesQuery.data?.find(c => c.id === rubro.categoryId)?.name || '-'}
                    </td>
                    <td className="p-3 font-medium text-xs text-primary">
                      {apusesQuery.data?.find(a => a.id === rubro.baseApuId)?.code || 'Ninguno'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* APU EDITOR SUB-TAB */}
      {activeSubTab === 'apus' && (
        <div className="grid gap-6 lg:grid-cols-[280px_1fr]">
          {/* APUs List */}
          <div className="rounded-xl border border-border bg-panel p-4 space-y-4">
            <div className="flex justify-between items-center border-b border-border pb-2">
              <h3 className="font-bold text-sm">Lista de APUs</h3>
              <Button variant="ghost" size="icon" onClick={() => setShowApuModal(true)}>
                <Plus className="size-4 text-primary" />
              </Button>
            </div>
            <ul className="space-y-1 max-h-96 overflow-y-auto">
              {apusesQuery.data?.map(apu => (
                <li
                  key={apu.id}
                  onClick={() => setSelectedApuId(apu.id!)}
                  className={`px-3 py-2 rounded-lg cursor-pointer text-xs font-medium transition-colors hover:bg-accent/40 flex justify-between items-center ${
                    selectedApuId === apu.id ? 'bg-primary/10 text-primary border border-primary/20' : 'text-foreground border border-transparent'
                  }`}
                >
                  <div>
                    <span className="font-mono font-bold block">{apu.code}</span>
                    <span className="truncate block max-w-44 text-muted-foreground">{apu.name}</span>
                  </div>
                  <span className="text-[10px] bg-secondary px-1.5 py-0.5 rounded">
                    $ {apu.estimatedCost?.toFixed(2) || '0.00'}
                  </span>
                </li>
              ))}
            </ul>
          </div>

          {/* APU Components Editor */}
          <div className="space-y-4">
            {selectedApuId ? (
              <div className="rounded-xl border border-border bg-panel p-5 space-y-6">
                <div className="flex justify-between items-center border-b border-border pb-3">
                  <div>
                    <span className="text-xs text-muted-foreground font-mono">
                      APU Maestro: {apusesQuery.data?.find(a => a.id === selectedApuId)?.code}
                    </span>
                    <h2 className="text-lg font-bold">
                      {apusesQuery.data?.find(a => a.id === selectedApuId)?.name}
                    </h2>
                  </div>
                  <Button size="sm" onClick={() => setShowComponentModal(true)}>
                    <Plus className="size-4" />
                    Agregar Insumo Componente
                  </Button>
                </div>

                <div className="overflow-x-auto">
                  <table className="w-full text-left text-sm border-collapse">
                    <thead>
                      <tr className="border-b border-border text-muted-foreground text-xs font-semibold">
                        <th className="py-2">Sección</th>
                        <th className="py-2">Insumo / Descripción</th>
                        <th className="py-2">Rend. / Desp.</th>
                        <th className="py-2">Cantidad</th>
                        <th className="py-2">Precio Unit.</th>
                        <th className="py-2 text-right">Costo / Rend.</th>
                        <th className="py-2 text-right"></th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-border">
                      {apuComponentsQuery.data?.map(comp => {
                        const isLaborOrEquipment = comp.section === 'MANO_DE_OBRA' || comp.section === 'EQUIPOS'
                        const apuYield = apusesQuery.data?.find(a => a.id === selectedApuId)?.yield || 1
                        const effectiveYield = comp.yield && comp.yield > 0 ? comp.yield : apuYield
                        
                        // Component Cost Calculation
                        const qty = comp.quantity || 0
                        const price = comp.unitPrice || 0
                        const waste = comp.wasteFactor || 0
                        const lineTotal = isLaborOrEquipment
                          ? (qty * price * (1 + waste)) / effectiveYield
                          : qty * price * (1 + waste)

                        return (
                          <tr key={comp.id} className="hover:bg-accent/20">
                            <td className="py-3 text-[11px] font-bold text-muted-foreground">{comp.section}</td>
                            <td className="py-3">
                              <p className="font-medium text-xs">{comp.description}</p>
                              <span className="text-[10px] text-muted-foreground">
                                Unidad: {unitsQuery.data?.find(u => u.id === comp.unitId)?.code || '-'}
                              </span>
                            </td>
                            <td className="py-3 text-xs">
                              {comp.yield && `R: ${comp.yield}`} {comp.wasteFactor && comp.wasteFactor > 0 ? `| D: ${(comp.wasteFactor * 100).toFixed(0)}%` : ''}
                            </td>
                            <td className="py-3 text-xs font-medium">{comp.quantity}</td>
                            <td className="py-3 text-xs font-medium">${comp.unitPrice?.toFixed(4)}</td>
                            <td className="py-3 text-right font-semibold text-xs text-foreground">
                              $ {lineTotal.toFixed(4)}
                            </td>
                            <td className="py-3 text-right">
                              <Button
                                variant="ghost"
                                size="icon"
                                className="size-7 text-muted-foreground hover:text-destructive"
                                onClick={() => removeComponentMutation.mutate({ params: { path: { apuId: selectedApuId!, componentId: comp.id! } } })}
                                disabled={removeComponentMutation.isPending}
                              >
                                <Trash2 className="size-3.5" />
                              </Button>
                            </td>
                          </tr>
                        )
                      })}
                      {(!apuComponentsQuery.data || apuComponentsQuery.data.length === 0) && (
                        <tr>
                          <td colSpan={7} className="py-8 text-center text-muted-foreground">
                            Este APU no tiene insumos componentes asignados.
                          </td>
                        </tr>
                      )}
                    </tbody>
                  </table>
                </div>
              </div>
            ) : (
              <div className="rounded-xl border border-dashed border-border bg-panel/30 p-12 text-center text-muted-foreground h-64 flex flex-col items-center justify-center">
                <Hammer className="size-8 text-muted-foreground/45 mb-2" />
                Selecciona un APU de la lista para gestionar sus componentes y calcular su costo unitario.
              </div>
            )}
          </div>
        </div>
      )}

      {/* CONFIGURATION SUB-TAB */}
      {activeSubTab === 'config' && (
        <div className="grid gap-6 md:grid-cols-3">
          {/* Units */}
          <div className="rounded-xl border border-border bg-panel p-5 space-y-4">
            <h3 className="font-bold text-sm border-b border-border pb-2 flex items-center gap-1.5">
              <Settings2 className="size-4 text-primary" />
              Unidades de Medida
            </h3>
            <form onSubmit={unitForm.handleSubmit(handleCreateUnit)} className="flex gap-2">
              <Input placeholder="Código" {...unitForm.register('code')} className="w-20" />
              <Input placeholder="Nombre" {...unitForm.register('name')} className="flex-1" />
              <Button type="submit" size="sm" disabled={createUnitMutation.isPending}>
                +
              </Button>
            </form>
            <ul className="divide-y divide-border max-h-48 overflow-y-auto text-xs">
              {unitsQuery.data?.map(u => (
                <li key={u.id} className="py-2 flex justify-between">
                  <span className="font-mono font-bold text-primary">{u.code}</span>
                  <span className="text-muted-foreground">{u.name}</span>
                </li>
              ))}
            </ul>
          </div>

          {/* Categories */}
          <div className="rounded-xl border border-border bg-panel p-5 space-y-4">
            <h3 className="font-bold text-sm border-b border-border pb-2 flex items-center gap-1.5">
              <Settings className="size-4 text-primary" />
              Categorías de Catálogo
            </h3>
            <form onSubmit={categoryForm.handleSubmit(handleCreateCategory)} className="flex gap-2">
              <Input placeholder="Código" {...categoryForm.register('code')} className="w-20" />
              <Input placeholder="Nombre" {...categoryForm.register('name')} className="flex-1" />
              <Button type="submit" size="sm" disabled={createCategoryMutation.isPending}>
                +
              </Button>
            </form>
            <ul className="divide-y divide-border max-h-48 overflow-y-auto text-xs">
              {categoriesQuery.data?.map(c => (
                <li key={c.id} className="py-2 flex justify-between">
                  <span className="font-mono font-bold text-primary">{c.code}</span>
                  <span className="text-muted-foreground">{c.name}</span>
                </li>
              ))}
            </ul>
          </div>

          {/* Taxes */}
          <div className="rounded-xl border border-border bg-panel p-5 space-y-4">
            <h3 className="font-bold text-sm border-b border-border pb-2 flex items-center gap-1.5">
              <Percent className="size-4 text-primary" />
              Impuestos Configurados
            </h3>
            <form onSubmit={taxForm.handleSubmit(handleCreateTax)} className="space-y-2">
              <div className="flex gap-2">
                <Input placeholder="Cod" {...taxForm.register('code')} className="w-16" />
                <Input placeholder="Nombre" {...taxForm.register('name')} className="flex-1" />
              </div>
              <div className="flex gap-2">
                <Input type="number" step="0.01" placeholder="Tasa %" {...taxForm.register('rate')} className="flex-1" />
                <Button type="submit" size="sm" disabled={createTaxMutation.isPending} className="w-12">
                  Crear
                </Button>
              </div>
            </form>
            <ul className="divide-y divide-border max-h-48 overflow-y-auto text-xs">
              {taxesQuery.data?.map(t => (
                <li key={t.id} className="py-2 flex justify-between">
                  <div>
                    <span className="font-mono font-bold text-primary">{t.code}</span>
                    <span className="text-muted-foreground ml-2">{t.name}</span>
                  </div>
                  <span className="font-semibold">{((t.rate || 0) * 100).toFixed(2)}%</span>
                </li>
              ))}
            </ul>
          </div>
        </div>
      )}

      {/* Insumo Modal */}
      {showInsumoModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs overflow-y-auto">
          <div className="w-full max-w-md bg-panel border border-border p-6 rounded-xl shadow-lg my-8 space-y-4 animate-scale-up">
            <h2 className="text-lg font-bold border-b border-border pb-2">Crear Insumo Comercial</h2>
            <form onSubmit={insumoForm.handleSubmit(handleCreateInsumo)} className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="text-xs font-semibold text-muted-foreground mb-1 block">Código</label>
                  <Input placeholder="Ej. MAT-CEM-01" {...insumoForm.register('code')} />
                </div>
                <div>
                  <label className="text-xs font-semibold text-muted-foreground mb-1 block">Nombre</label>
                  <Input placeholder="Ej. Cemento Portland 50kg" {...insumoForm.register('name')} />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="text-xs font-semibold text-muted-foreground mb-1 block">Tipo de Insumo</label>
                  <select
                    className="flex h-9 w-full rounded-md border border-border bg-background px-3 py-1 text-sm shadow-xs focus-visible:outline-hidden"
                    {...insumoForm.register('type')}
                  >
                    <option value="MATERIAL">Material</option>
                    <option value="MANO_DE_OBRA">Mano de Obra</option>
                    <option value="EQUIPO">Equipo/Maquinaria</option>
                    <option value="TRANSPORTE">Transporte</option>
                    <option value="SUBCONTRATO">Subcontrato</option>
                    <option value="OTRO">Otro</option>
                  </select>
                </div>
                <div>
                  <label className="text-xs font-semibold text-muted-foreground mb-1 block">Unidad de Medida</label>
                  <select
                    className="flex h-9 w-full rounded-md border border-border bg-background px-3 py-1 text-sm shadow-xs focus-visible:outline-hidden"
                    {...insumoForm.register('unitId')}
                  >
                    <option value="">Seleccione...</option>
                    {unitsQuery.data?.map(u => (
                      <option key={u.id} value={u.id}>{u.code} - {u.name}</option>
                    ))}
                  </select>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="text-xs font-semibold text-muted-foreground mb-1 block">Precio de Referencia</label>
                  <Input type="number" step="0.0001" {...insumoForm.register('referencePrice')} />
                </div>
                <div>
                  <label className="text-xs font-semibold text-muted-foreground mb-1 block">Categoría</label>
                  <select
                    className="flex h-9 w-full rounded-md border border-border bg-background px-3 py-1 text-sm shadow-xs focus-visible:outline-hidden"
                    {...insumoForm.register('categoryId')}
                  >
                    <option value="">Sin categoría...</option>
                    {categoriesQuery.data?.map(c => (
                      <option key={c.id} value={c.id}>{c.name}</option>
                    ))}
                  </select>
                </div>
              </div>

              <div className="flex justify-end gap-2 border-t border-border pt-3">
                <Button type="button" variant="outline" onClick={() => setShowInsumoModal(false)}>
                  Cancelar
                </Button>
                <Button type="submit" disabled={createInsumoMutation.isPending}>
                  Crear Insumo
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* APU Modal */}
      {showApuModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs">
          <div className="w-full max-w-md bg-panel border border-border p-6 rounded-xl shadow-lg space-y-4 animate-scale-up">
            <h2 className="text-lg font-bold border-b border-border pb-2">Crear Nuevo APU Maestro</h2>
            <form onSubmit={apuForm.handleSubmit(handleCreateApu)} className="space-y-4">
              <div>
                <label className="text-xs font-semibold text-muted-foreground mb-1 block">Código</label>
                <Input placeholder="Ej. APU-VIGA-01" {...apuForm.register('code')} />
              </div>
              <div>
                <label className="text-xs font-semibold text-muted-foreground mb-1 block">Nombre/Descripción APU</label>
                <Input placeholder="Ej. Viga de Hormigón Armado" {...apuForm.register('name')} />
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="text-xs font-semibold text-muted-foreground mb-1 block">Unidad de Medida</label>
                  <select
                    className="flex h-9 w-full rounded-md border border-border bg-background px-3 py-1 text-sm shadow-xs focus-visible:outline-hidden"
                    {...apuForm.register('unitId')}
                  >
                    <option value="">Seleccione...</option>
                    {unitsQuery.data?.map(u => (
                      <option key={u.id} value={u.id}>{u.code} - {u.name}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="text-xs font-semibold text-muted-foreground mb-1 block">Rendimiento Base</label>
                  <Input type="number" step="0.0001" {...apuForm.register('yield')} />
                </div>
              </div>

              <div className="flex justify-end gap-2 border-t border-border pt-3">
                <Button type="button" variant="outline" onClick={() => setShowApuModal(false)}>
                  Cancelar
                </Button>
                <Button type="submit" disabled={createApuMutation.isPending}>
                  Crear APU
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* APU Component Modal */}
      {showComponentModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs overflow-y-auto">
          <div className="w-full max-w-md bg-panel border border-border p-6 rounded-xl shadow-lg my-8 space-y-4 animate-scale-up">
            <h2 className="text-lg font-bold border-b border-border pb-2">Agregar Insumo a APU</h2>
            <form
              onSubmit={componentForm.handleSubmit(handleAddComponent)}
              className="space-y-4"
            >
              <div>
                <label className="text-xs font-semibold text-muted-foreground mb-1 block">Sección</label>
                <select
                  className="flex h-9 w-full rounded-md border border-border bg-background px-3 py-1 text-sm shadow-xs focus-visible:outline-hidden"
                  {...componentForm.register('section')}
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
                  {...componentForm.register('insumoId')}
                  onChange={(e) => {
                    const ins = insumosQuery.data?.find(i => i.id === e.target.value)
                    if (ins) {
                      componentForm.setValue('unitId', ins.unitId!)
                      componentForm.setValue('unitPrice', ins.referencePrice || 0)
                      componentForm.setValue('description', ins.name!)
                    }
                  }}
                >
                  <option value="">Seleccione insumo comercial...</option>
                  {insumosQuery.data?.map(i => (
                    <option key={i.id} value={i.id}>[{i.code}] {i.name} - ${i.referencePrice?.toFixed(2)}</option>
                  ))}
                </select>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="text-xs font-semibold text-muted-foreground mb-1 block">Cantidad</label>
                  <Input type="number" step="0.0001" {...componentForm.register('quantity')} />
                </div>
                <div>
                  <label className="text-xs font-semibold text-muted-foreground mb-1 block">Unidad</label>
                  <select
                    className="flex h-9 w-full rounded-md border border-border bg-background px-3 py-1 text-sm shadow-xs focus-visible:outline-hidden"
                    {...componentForm.register('unitId')}
                  >
                    <option value="">Seleccione...</option>
                    {unitsQuery.data?.map(u => (
                      <option key={u.id} value={u.id}>{u.code}</option>
                    ))}
                  </select>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="text-xs font-semibold text-muted-foreground mb-1 block">Rendimiento (Mano Obra/Equipos)</label>
                  <Input type="number" step="0.0001" {...componentForm.register('yield')} />
                </div>
                <div>
                  <label className="text-xs font-semibold text-muted-foreground mb-1 block">Desperdicio (Factor 0..1)</label>
                  <Input type="number" step="0.000001" {...componentForm.register('wasteFactor')} />
                </div>
              </div>

              <div>
                <label className="text-xs font-semibold text-muted-foreground mb-1 block">Precio Unitario Ajustado ($)</label>
                <Input type="number" step="0.0001" {...componentForm.register('unitPrice')} />
              </div>

              <div className="flex justify-end gap-2 border-t border-border pt-3">
                <Button type="button" variant="outline" onClick={() => setShowComponentModal(false)}>
                  Cancelar
                </Button>
                <Button type="submit" disabled={addComponentMutation.isPending}>
                  Agregar
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Rubro Modal */}
      {showRubroModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs overflow-y-auto">
          <div className="w-full max-w-md bg-panel border border-border p-6 rounded-xl shadow-lg my-8 space-y-4 animate-scale-up">
            <h2 className="text-lg font-bold border-b border-border pb-2">Crear Rubro Maestro</h2>
            <form onSubmit={rubroForm.handleSubmit(handleCreateRubro)} className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="text-xs font-semibold text-muted-foreground mb-1 block">Código</label>
                  <Input placeholder="Ej. RUB-MAMP-01" {...rubroForm.register('code')} />
                </div>
                <div>
                  <label className="text-xs font-semibold text-muted-foreground mb-1 block">Nombre</label>
                  <Input placeholder="Ej. Mampostería de Ladrillo" {...rubroForm.register('name')} />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="text-xs font-semibold text-muted-foreground mb-1 block">Unidad de Medida</label>
                  <select
                    className="flex h-9 w-full rounded-md border border-border bg-background px-3 py-1 text-sm shadow-xs focus-visible:outline-hidden"
                    {...rubroForm.register('unitId')}
                  >
                    <option value="">Seleccione...</option>
                    {unitsQuery.data?.map(u => (
                      <option key={u.id} value={u.id}>{u.code} - {u.name}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="text-xs font-semibold text-muted-foreground mb-1 block">Categoría</label>
                  <select
                    className="flex h-9 w-full rounded-md border border-border bg-background px-3 py-1 text-sm shadow-xs focus-visible:outline-hidden"
                    {...rubroForm.register('categoryId')}
                  >
                    <option value="">Seleccione...</option>
                    {categoriesQuery.data?.map(c => (
                      <option key={c.id} value={c.id}>{c.name}</option>
                    ))}
                  </select>
                </div>
              </div>

              <div>
                <label className="text-xs font-semibold text-muted-foreground mb-1 block">APU de Referencia (Análisis de Precio)</label>
                <select
                  className="flex h-9 w-full rounded-md border border-border bg-background px-3 py-1 text-sm shadow-xs focus-visible:outline-hidden"
                  {...rubroForm.register('baseApuId')}
                >
                  <option value="">Sin APU asignado...</option>
                  {apusesQuery.data?.map(a => (
                    <option key={a.id} value={a.id}>[{a.code}] {a.name} - ${a.estimatedCost?.toFixed(2)}</option>
                  ))}
                </select>
              </div>

              <div>
                <label className="text-xs font-semibold text-muted-foreground mb-1 block">Especificación Técnica</label>
                <textarea
                  className="flex min-h-16 w-full rounded-md border border-border bg-background px-3 py-2 text-sm shadow-xs focus-visible:outline-hidden"
                  placeholder="Especificación detallada de obra..."
                  {...rubroForm.register('specification')}
                />
              </div>

              <div className="flex justify-end gap-2 border-t border-border pt-3">
                <Button type="button" variant="outline" onClick={() => setShowRubroModal(false)}>
                  Cancelar
                </Button>
                <Button type="submit" disabled={createRubroMutation.isPending}>
                  Crear Rubro
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}
