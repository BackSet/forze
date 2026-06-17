import { useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm, useFieldArray, type Resolver } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import * as zod from 'zod'
import { toast } from 'sonner'
import { Truck, FileSpreadsheet, Plus, Trash2, Award } from 'lucide-react'

import { api } from '@/lib/api/client'
import { apiErrorMessage } from '@/lib/api/errors'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { useSessionStore } from '@/lib/auth/session-store'

const supplierSchema = zod.object({
  legalName: zod.string().min(3, 'Mínimo 3 caracteres').max(200),
  taxId: zod.string().optional(),
  contactName: zod.string().optional(),
  phone: zod.string().optional(),
  email: zod.string().email('Email inválido').or(zod.string().length(0)),
  city: zod.string().optional(),
  offeredProducts: zod.string().optional(),
  paymentTerms: zod.string().optional(),
  deliveryTime: zod.string().optional(),
  rating: zod.coerce.number().min(0).max(5).default(5),
})
type SupplierFormValues = zod.infer<typeof supplierSchema>

const quotationItemSchema = zod.object({
  insumoId: zod.string().uuid('Seleccione insumo'),
  description: zod.string().optional(),
  unitId: zod.string().uuid('Seleccione unidad'),
  unitPrice: zod.coerce.number().min(0.0001, 'Mínimo 0.0001'),
  minOrder: zod.coerce.number().default(0),
  discount: zod.coerce.number().default(0),
  taxesIncluded: zod.boolean().default(true),
  transportIncluded: zod.boolean().default(true),
})

const quotationSchema = zod.object({
  supplierId: zod.string().uuid('Seleccione proveedor'),
  quotationDate: zod.string().min(1, 'Requerido'),
  validUntil: zod.string().optional(),
  currencyCode: zod.string().length(3).default('USD'),
  taxConfigId: zod.string().uuid('Seleccione impuesto').optional().or(zod.string().length(0)),
  transportAmount: zod.coerce.number().default(0),
  conditions: zod.string().optional(),
  city: zod.string().optional(),
  items: zod.array(quotationItemSchema).min(1, 'Debe agregar al menos un ítem'),
})
type QuotationFormValues = zod.infer<typeof quotationSchema>

export function SuppliersTab() {
  const queryClient = useQueryClient()
  const activeOrgId = useSessionStore((state) => state.activeOrganizationId)
  
  const [activeSubTab, setActiveSubTab] = useState<'suppliers' | 'quotations' | 'history'>('suppliers')
  const [selectedSupplierId, setSelectedSupplierId] = useState<string | null>(null)
  const [selectedInsumoId, setSelectedInsumoId] = useState<string | null>(null)
  
  const [showSupplierModal, setShowSupplierModal] = useState(false)
  const [showQuotationModal, setShowQuotationModal] = useState(false)

  // Queries
  const suppliersQuery = useQuery({
    ...api.queryOptions('get', '/api/suppliers'),
    enabled: !!activeOrgId,
  })

  const quotationsQuery = useQuery({
    ...api.queryOptions('get', '/api/quotations'),
    enabled: !!activeOrgId,
  })

  const insumosQuery = useQuery({
    ...api.queryOptions('get', '/api/insumos'),
    enabled: !!activeOrgId,
  })

  const unitsQuery = useQuery({
    ...api.queryOptions('get', '/api/admin/units'),
    enabled: !!activeOrgId,
  })

  const taxesQuery = useQuery({
    ...api.queryOptions('get', '/api/admin/taxes'),
    enabled: !!activeOrgId,
  })

  const priceHistoryQuery = useQuery({
    ...api.queryOptions('get', '/api/price-history/{insumoId}', { params: { path: { insumoId: selectedInsumoId || '' } } }),
    enabled: !!selectedInsumoId,
  })

  // Mutations
  const createSupplierMutation = api.useMutation('post', '/api/suppliers', {
    onSuccess: () => {
      toast.success('Proveedor creado con éxito')
      queryClient.invalidateQueries({ queryKey: ['get', '/api/suppliers'] })
      setShowSupplierModal(false)
      supplierForm.reset()
    },
    onError: (err) => toast.error(apiErrorMessage(err, 'Error al crear proveedor')),
  })

  const deactivateSupplierMutation = api.useMutation('put', '/api/suppliers/{id}/deactivate', {
    onSuccess: () => {
      toast.success('Proveedor desactivado')
      queryClient.invalidateQueries({ queryKey: ['get', '/api/suppliers'] })
    },
    onError: (err) => toast.error(apiErrorMessage(err, 'Error al desactivar proveedor')),
  })

  const createQuotationMutation = api.useMutation('post', '/api/quotations', {
    onSuccess: () => {
      toast.success('Cotización registrada con éxito')
      queryClient.invalidateQueries({ queryKey: ['get', '/api/quotations'] })
      setShowQuotationModal(false)
      quotationForm.reset()
    },
    onError: (err) => toast.error(apiErrorMessage(err, 'Error al crear cotización')),
  })

  // Forms without generic parameters
  const supplierForm = useForm({
    resolver: zodResolver(supplierSchema) as unknown as Resolver<SupplierFormValues>,
    defaultValues: {
      legalName: '', taxId: '', contactName: '', phone: '', email: '',
      city: '', offeredProducts: '', paymentTerms: '', deliveryTime: '', rating: 5
    }
  })

  const quotationForm = useForm({
    resolver: zodResolver(quotationSchema) as unknown as Resolver<QuotationFormValues>,
    defaultValues: {
      supplierId: '', quotationDate: new Date().toISOString().substring(0, 10), validUntil: '',
      currencyCode: 'USD', taxConfigId: '', transportAmount: 0, conditions: '', city: '',
      items: [] as zod.infer<typeof quotationItemSchema>[]
    }
  })

  const { fields, append, remove } = useFieldArray({
    control: quotationForm.control,
    name: 'items',
  })

  function handleCreateSupplier(values: SupplierFormValues) {
    createSupplierMutation.mutate({
      body: {
        legalName: values.legalName,
        rating: values.rating,
        ...(values.taxId ? { taxId: values.taxId } : {}),
        ...(values.contactName ? { contactName: values.contactName } : {}),
        ...(values.phone ? { phone: values.phone } : {}),
        ...(values.email ? { email: values.email } : {}),
        ...(values.city ? { city: values.city } : {}),
        ...(values.offeredProducts ? { offeredProducts: values.offeredProducts } : {}),
        ...(values.paymentTerms ? { paymentTerms: values.paymentTerms } : {}),
        ...(values.deliveryTime ? { deliveryTime: values.deliveryTime } : {}),
      }
    })
  }

  function handleCreateQuotation(values: QuotationFormValues) {
    createQuotationMutation.mutate({
      body: {
        supplierId: values.supplierId,
        quotationDate: values.quotationDate,
        currencyCode: values.currencyCode,
        transportAmount: values.transportAmount,
        ...(values.validUntil ? { validUntil: values.validUntil } : {}),
        ...(values.taxConfigId ? { taxConfigId: values.taxConfigId } : {}),
        ...(values.conditions ? { conditions: values.conditions } : {}),
        ...(values.city ? { city: values.city } : {}),
        items: values.items.map(item => {
          const desc = item.description || insumosQuery.data?.find(i => i.id === item.insumoId)?.name;
          return {
            insumoId: item.insumoId,
            unitId: item.unitId,
            unitPrice: item.unitPrice,
            minOrder: item.minOrder,
            discount: item.discount,
            taxesIncluded: item.taxesIncluded,
            transportIncluded: item.transportIncluded,
            ...(desc ? { description: desc } : {}),
          };
        })
      }
    })
  }

  const selectedSupplier = suppliersQuery.data?.find(s => s.id === selectedSupplierId)

  return (
    <div className="space-y-6 animate-fade-in">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 border-b border-border pb-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Proveedores y Cotizaciones</h1>
          <p className="text-sm text-muted-foreground">Administra la base de proveedores, registra cotizaciones e investiga precios históricos.</p>
        </div>

        {/* Sub-tabs */}
        <div className="flex bg-secondary p-0.5 rounded-lg border border-border">
          <Button
            variant={activeSubTab === 'suppliers' ? 'default' : 'ghost'}
            size="sm"
            onClick={() => setActiveSubTab('suppliers')}
          >
            Proveedores
          </Button>
          <Button
            variant={activeSubTab === 'quotations' ? 'default' : 'ghost'}
            size="sm"
            onClick={() => setActiveSubTab('quotations')}
          >
            Cotizaciones
          </Button>
          <Button
            variant={activeSubTab === 'history' ? 'default' : 'ghost'}
            size="sm"
            onClick={() => setActiveSubTab('history')}
          >
            Historial Precios
          </Button>
        </div>
      </div>

      {/* PROVEEDORES SUB-TAB */}
      {activeSubTab === 'suppliers' && (
        <div className="grid gap-6 lg:grid-cols-[1fr_360px]">
          {/* Suppliers List */}
          <div className="space-y-4">
            <div className="flex justify-between items-center">
              <h2 className="text-lg font-bold flex items-center gap-2">
                <Truck className="size-5 text-primary" />
                Proveedores Registrados
              </h2>
              <Button size="sm" onClick={() => setShowSupplierModal(true)}>
                <Plus className="size-4" />
                Registrar Proveedor
              </Button>
            </div>

            {suppliersQuery.isLoading ? (
              <div className="h-10 w-full animate-pulse bg-muted rounded-xl" />
            ) : (
              <div className="grid gap-3 sm:grid-cols-2">
                {suppliersQuery.data?.map(supplier => (
                  <div
                    key={supplier.id}
                    onClick={() => setSelectedSupplierId(supplier.id!)}
                    className={`rounded-xl border p-4 cursor-pointer transition-all hover:bg-accent/40 ${
                      selectedSupplierId === supplier.id ? 'border-primary bg-primary/5' : 'border-border bg-panel'
                    }`}
                  >
                    <div className="flex justify-between items-start">
                      <h3 className="font-semibold text-sm">{supplier.legalName}</h3>
                      <span className={`inline-flex rounded-full px-2 py-0.5 text-[10px] font-medium ${
                        supplier.status === 'ACTIVO' ? 'bg-emerald-500/10 text-emerald-500' : 'bg-destructive/10 text-destructive'
                      }`}>
                        {supplier.status}
                      </span>
                    </div>
                    <p className="text-xs text-muted-foreground mt-1">RUC/Tax ID: {supplier.taxId || '-'}</p>
                    <div className="flex items-center gap-1 text-xs text-primary mt-2">
                      <Award className="size-3.5" />
                      <span>Calificación: {supplier.rating || 5}/5</span>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Supplier Details Side Panel */}
          <div>
            {selectedSupplierId && selectedSupplier ? (
              <div className="rounded-xl border border-border bg-panel p-5 space-y-4 animate-fade-in">
                <div className="border-b border-border pb-3 flex justify-between items-start">
                  <div>
                    <h2 className="text-lg font-bold">{selectedSupplier.legalName}</h2>
                    <p className="text-xs text-muted-foreground">RUC: {selectedSupplier.taxId || '-'}</p>
                  </div>
                  {selectedSupplier.status === 'ACTIVO' && (
                    <Button
                      variant="ghost"
                      size="sm"
                      className="text-destructive hover:bg-destructive/10"
                      onClick={() => deactivateSupplierMutation.mutate({ params: { path: { id: selectedSupplier.id! } } })}
                      disabled={deactivateSupplierMutation.isPending}
                    >
                      Desactivar
                    </Button>
                  )}
                </div>

                <div className="space-y-3 text-xs text-foreground">
                  <div>
                    <span className="font-semibold text-muted-foreground block">Calificación</span>
                    <span className="font-semibold">{selectedSupplier.rating || 5} / 5 estrellas</span>
                  </div>
                </div>
              </div>
            ) : (
              <div className="rounded-xl border border-dashed border-border bg-panel/30 p-8 text-center text-muted-foreground flex flex-col items-center justify-center h-48">
                <Truck className="size-8 text-muted-foreground/50 mb-2" />
                Selecciona un proveedor de la lista para ver su información de contacto y condiciones.
              </div>
            )}
          </div>
        </div>
      )}

      {/* COTIZACIONES SUB-TAB */}
      {activeSubTab === 'quotations' && (
        <div className="space-y-4">
          <div className="flex justify-between items-center">
            <h2 className="text-lg font-bold flex items-center gap-2">
              <FileSpreadsheet className="size-5 text-primary" />
              Cotizaciones de Materiales y Servicios
            </h2>
            <Button size="sm" onClick={() => setShowQuotationModal(true)}>
              <Plus className="size-4" />
              Registrar Cotización
            </Button>
          </div>

          <div className="overflow-x-auto rounded-xl border border-border bg-panel">
            <table className="w-full text-left text-sm border-collapse">
              <thead>
                <tr className="border-b border-border text-muted-foreground text-xs font-semibold">
                  <th className="p-3">Proveedor</th>
                  <th className="p-3">Fecha Cotización</th>
                  <th className="p-3">Validez hasta</th>
                  <th className="p-3">Moneda</th>
                  <th className="p-3">Estado</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {quotationsQuery.data?.map(q => (
                  <tr key={q.id} className="hover:bg-accent/30 transition-colors">
                    <td className="p-3 font-medium">
                      {suppliersQuery.data?.find(s => s.id === q.supplierId)?.legalName || 'Proveedor'}
                    </td>
                    <td className="p-3 text-xs">{q.quotationDate}</td>
                    <td className="p-3 text-xs">{q.validUntil || '-'}</td>
                    <td className="p-3 font-mono font-bold text-xs">{q.currencyCode}</td>
                    <td className="p-3">
                      <span className={`inline-flex rounded-full px-2 py-0.5 text-xs font-medium bg-emerald-500/10 text-emerald-500`}>
                        {q.status}
                      </span>
                    </td>
                  </tr>
                ))}
                {(!quotationsQuery.data || quotationsQuery.data.length === 0) && (
                  <tr>
                    <td colSpan={5} className="py-8 text-center text-muted-foreground">
                      No hay cotizaciones registradas. Crea una para ingresar precios históricos al sistema.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* HISTORIAL PRECIOS SUB-TAB */}
      {activeSubTab === 'history' && (
        <div className="grid gap-6 lg:grid-cols-[280px_1fr]">
          {/* Select Insumo */}
          <div className="rounded-xl border border-border bg-panel p-4 space-y-4">
            <h3 className="font-bold text-sm border-b border-border pb-2">Insumo del Catálogo</h3>
            <ul className="space-y-1 max-h-96 overflow-y-auto">
              {insumosQuery.data?.map(i => (
                <li
                  key={i.id}
                  onClick={() => setSelectedInsumoId(i.id!)}
                  className={`px-3 py-2 rounded-lg cursor-pointer text-xs font-medium transition-colors hover:bg-accent/40 ${
                    selectedInsumoId === i.id ? 'bg-primary/10 text-primary border border-primary/20' : 'text-foreground border border-transparent'
                  }`}
                >
                  <span className="font-mono font-bold block text-[10px]">{i.code}</span>
                  <span className="truncate block">{i.name}</span>
                </li>
              ))}
            </ul>
          </div>

          {/* Price History List */}
          <div>
            {selectedInsumoId ? (
              <div className="rounded-xl border border-border bg-panel p-5 space-y-4">
                <h2 className="text-lg font-bold">
                  Historial de Precios de Cotización: {insumosQuery.data?.find(i => i.id === selectedInsumoId)?.name}
                </h2>

                <div className="overflow-x-auto">
                  <table className="w-full text-left text-sm border-collapse">
                    <thead>
                      <tr className="border-b border-border text-muted-foreground text-xs font-semibold">
                        <th className="py-2">Fecha</th>
                        <th className="py-2">Precio Cotizado</th>
                        <th className="py-2">Validez</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-border">
                      {priceHistoryQuery.data?.map(h => (
                        <tr key={h.id} className="hover:bg-accent/20">
                          <td className="py-3 text-xs">{h.priceDate}</td>
                          <td className="py-3 text-xs font-bold text-foreground">
                            {h.currencyCode} {h.price?.toFixed(4)}
                          </td>
                          <td className="py-3 text-xs text-muted-foreground">{h.validUntil || 'Indefinida'}</td>
                        </tr>
                      ))}
                      {(!priceHistoryQuery.data || priceHistoryQuery.data.length === 0) && (
                        <tr>
                          <td colSpan={3} className="py-8 text-center text-muted-foreground">
                            No hay registros de precios de cotizaciones para este insumo.
                          </td>
                        </tr>
                      )}
                    </tbody>
                  </table>
                </div>
              </div>
            ) : (
              <div className="rounded-xl border border-dashed border-border bg-panel/30 p-12 text-center text-muted-foreground h-64 flex flex-col items-center justify-center">
                <FileSpreadsheet className="size-8 text-muted-foreground/45 mb-2" />
                Selecciona un insumo para ver la evolución de sus precios históricos cotizados por proveedores.
              </div>
            )}
          </div>
        </div>
      )}

      {/* Supplier Modal */}
      {showSupplierModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs">
          <div className="w-full max-w-md bg-panel border border-border p-6 rounded-xl shadow-lg space-y-4 animate-scale-up">
            <h2 className="text-lg font-bold border-b border-border pb-2">Registrar Proveedor</h2>
            <form onSubmit={supplierForm.handleSubmit((v) => handleCreateSupplier(v))} className="space-y-4">
              <div>
                <label className="text-xs font-semibold text-muted-foreground mb-1 block">Razón Social</label>
                <Input placeholder="Ej. Distribuidora Metalúrgica C.A." {...supplierForm.register('legalName')} />
                {supplierForm.formState.errors.legalName && (
                  <p className="mt-1 text-xs text-destructive">{supplierForm.formState.errors.legalName.message}</p>
                )}
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="text-xs font-semibold text-muted-foreground mb-1 block">RUC / ID Tributario</label>
                  <Input placeholder="1792..." {...supplierForm.register('taxId')} />
                </div>
                <div>
                  <label className="text-xs font-semibold text-muted-foreground mb-1 block">Ciudad</label>
                  <Input placeholder="Quito" {...supplierForm.register('city')} />
                </div>
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="text-xs font-semibold text-muted-foreground mb-1 block">Teléfono</label>
                  <Input placeholder="099..." {...supplierForm.register('phone')} />
                </div>
                <div>
                  <label className="text-xs font-semibold text-muted-foreground mb-1 block">Calificación Inicial (1..5)</label>
                  <Input type="number" step="0.5" {...supplierForm.register('rating')} />
                </div>
              </div>
              <div>
                <label className="text-xs font-semibold text-muted-foreground mb-1 block">Email Contacto</label>
                <Input placeholder="ventas@proveedor.com" {...supplierForm.register('email')} />
                {supplierForm.formState.errors.email && (
                  <p className="mt-1 text-xs text-destructive">{supplierForm.formState.errors.email.message}</p>
                )}
              </div>

              <div className="flex justify-end gap-2 border-t border-border pt-3">
                <Button type="button" variant="outline" onClick={() => setShowSupplierModal(false)}>
                  Cancelar
                </Button>
                <Button type="submit" disabled={createSupplierMutation.isPending}>
                  Registrar
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Quotation Modal */}
      {showQuotationModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs overflow-y-auto">
          <div className="w-full max-w-2xl bg-panel border border-border p-6 rounded-xl shadow-lg my-8 space-y-4 animate-scale-up">
            <h2 className="text-lg font-bold border-b border-border pb-2">Registrar Cotización de Proveedor</h2>
            <form onSubmit={quotationForm.handleSubmit((v) => handleCreateQuotation(v))} className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="text-xs font-semibold text-muted-foreground mb-1 block">Proveedor</label>
                  <select
                    className="flex h-9 w-full rounded-md border border-border bg-background px-3 py-1 text-sm shadow-xs focus-visible:outline-hidden"
                    {...quotationForm.register('supplierId')}
                  >
                    <option value="">Seleccione proveedor...</option>
                    {suppliersQuery.data?.filter(s => s.status === 'ACTIVO').map(s => (
                      <option key={s.id} value={s.id}>{s.legalName}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="text-xs font-semibold text-muted-foreground mb-1 block">Impuesto Aplicable</label>
                  <select
                    className="flex h-9 w-full rounded-md border border-border bg-background px-3 py-1 text-sm shadow-xs focus-visible:outline-hidden"
                    {...quotationForm.register('taxConfigId')}
                  >
                    <option value="">Sin impuesto especificado...</option>
                    {taxesQuery.data?.map(t => (
                      <option key={t.id} value={t.id}>{t.name} ({((t.rate || 0)*100).toFixed(0)}%)</option>
                    ))}
                  </select>
                </div>
              </div>

              <div className="grid grid-cols-3 gap-4">
                <div>
                  <label className="text-xs font-semibold text-muted-foreground mb-1 block">Fecha Cotización</label>
                  <Input type="date" {...quotationForm.register('quotationDate')} />
                </div>
                <div>
                  <label className="text-xs font-semibold text-muted-foreground mb-1 block">Validez hasta</label>
                  <Input type="date" {...quotationForm.register('validUntil')} />
                </div>
                <div>
                  <label className="text-xs font-semibold text-muted-foreground mb-1 block">Ciudad origen</label>
                  <Input placeholder="Quito" {...quotationForm.register('city')} />
                </div>
              </div>

              {/* Items Table */}
              <div className="border border-border rounded-xl bg-background overflow-hidden p-4 space-y-3">
                <div className="flex justify-between items-center border-b border-border pb-2">
                  <h3 className="font-bold text-xs">Ítems de la Cotización</h3>
                  <Button
                    type="button"
                    size="sm"
                    variant="outline"
                    onClick={() => append({ insumoId: '', description: '', unitId: '', unitPrice: 0, minOrder: 0, discount: 0, taxesIncluded: true, transportIncluded: true })}
                  >
                    + Agregar ítem
                  </Button>
                </div>

                <div className="space-y-3 max-h-48 overflow-y-auto">
                  {fields.map((field, index) => (
                    <div key={field.id} className="flex gap-2 items-end border border-border/40 p-2 rounded-lg bg-panel">
                      <div className="flex-1 min-w-44">
                        <label className="text-[10px] text-muted-foreground block mb-0.5">Insumo</label>
                        <select
                          className="flex h-8 w-full rounded border border-border bg-background px-2 text-xs shadow-xs focus-visible:outline-hidden"
                          {...quotationForm.register(`items.${index}.insumoId` as const)}
                          onChange={(e) => {
                            const ins = insumosQuery.data?.find(i => i.id === e.target.value)
                            if (ins) {
                              quotationForm.setValue(`items.${index}.unitId`, ins.unitId!)
                              quotationForm.setValue(`items.${index}.unitPrice`, ins.referencePrice || 0)
                            }
                          }}
                        >
                          <option value="">Seleccione...</option>
                          {insumosQuery.data?.map(i => (
                            <option key={i.id} value={i.id}>{i.name}</option>
                          ))}
                        </select>
                      </div>
                      <div className="w-24">
                        <label className="text-[10px] text-muted-foreground block mb-0.5">Precio Unitario ($)</label>
                        <Input
                          type="number"
                          step="0.0001"
                          className="h-8 text-xs"
                          {...quotationForm.register(`items.${index}.unitPrice` as const)}
                        />
                      </div>
                      <div className="w-16">
                        <label className="text-[10px] text-muted-foreground block mb-0.5">Unidad</label>
                        <select
                          className="flex h-8 w-full rounded border border-border bg-background px-2 text-xs shadow-xs focus-visible:outline-hidden"
                          {...quotationForm.register(`items.${index}.unitId` as const)}
                        >
                          {unitsQuery.data?.map(u => (
                            <option key={u.id} value={u.id}>{u.code}</option>
                          ))}
                        </select>
                      </div>
                      <Button
                        type="button"
                        variant="ghost"
                        size="icon"
                        className="size-8 text-muted-foreground hover:text-destructive self-end"
                        onClick={() => remove(index)}
                      >
                        <Trash2 className="size-4" />
                      </Button>
                    </div>
                  ))}
                  {fields.length === 0 && (
                    <p className="text-center text-xs text-muted-foreground py-4">Agrega al menos un insumo cotizado.</p>
                  )}
                </div>
              </div>

              <div className="flex justify-end gap-2 border-t border-border pt-3">
                <Button type="button" variant="outline" onClick={() => setShowQuotationModal(false)}>
                  Cancelar
                </Button>
                <Button type="submit" disabled={createQuotationMutation.isPending}>
                  Registrar Cotización
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}
