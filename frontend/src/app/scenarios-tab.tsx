import { useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm, type Resolver } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import * as zod from 'zod'
import { toast } from 'sonner'
import { Plus, Star, CheckCircle, Scale, ShieldAlert } from 'lucide-react'

import { api } from '@/lib/api/client'
import { apiErrorMessage } from '@/lib/api/errors'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'

const scenarioSchema = zod.object({
  name: zod.string().min(3, 'Mínimo 3 caracteres').max(200),
  type: zod.enum(['BASE', 'ECONOMICO', 'RECOMENDADO', 'CONSERVADOR', 'PERSONALIZADO']),
  utilityRate: zod.coerce.number().min(0).max(100).default(10),
  indirectRate: zod.coerce.number().min(0).max(100).default(15),
  contingencyRate: zod.coerce.number().min(0).max(100).default(5),
  durationDays: zod.coerce.number().min(0).optional(),
  constructionMethod: zod.string().optional(),
})
type ScenarioFormValues = zod.infer<typeof scenarioSchema>

interface ScenariosTabProps {
  selectedVersionId: string | null
}

export function ScenariosTab({ selectedVersionId }: ScenariosTabProps) {
  const queryClient = useQueryClient()
  const [showModal, setShowModal] = useState(false)

  // Queries
  const scenariosQuery = useQuery({
    ...api.queryOptions('get', '/api/budget-versions/{versionId}/scenarios', { params: { path: { versionId: selectedVersionId || '' } } }),
    enabled: !!selectedVersionId,
  })

  // Mutations
  const createScenarioMutation = api.useMutation('post', '/api/budget-versions/{versionId}/scenarios', {
    onSuccess: () => {
      toast.success('Escenario creado correctamente')
      queryClient.invalidateQueries({
        queryKey: ['get', '/api/budget-versions/{versionId}/scenarios', { params: { path: { versionId: selectedVersionId! } } }]
      })
      setShowModal(false)
      scenarioForm.reset()
    },
    onError: (err) => toast.error(apiErrorMessage(err, 'Error al crear escenario')),
  })

  const makePrimaryMutation = api.useMutation('put', '/api/scenarios/{id}/primary', {
    onSuccess: () => {
      toast.success('Escenario seleccionado como Principal')
      queryClient.invalidateQueries({
        queryKey: ['get', '/api/budget-versions/{versionId}/scenarios', { params: { path: { versionId: selectedVersionId! } } }]
      })
      queryClient.invalidateQueries({
        queryKey: ['get', '/api/budget-versions/{id}', { params: { path: { id: selectedVersionId! } } }]
      })
    },
    onError: (err) => toast.error(apiErrorMessage(err, 'Error al designar escenario principal')),
  })

  // Forms
  const scenarioForm = useForm({
    resolver: zodResolver(scenarioSchema) as unknown as Resolver<ScenarioFormValues>,
    defaultValues: { name: '', type: 'BASE' as const, utilityRate: 10, indirectRate: 15, contingencyRate: 5, durationDays: 30, constructionMethod: '' }
  })

  function handleCreateScenario(values: ScenarioFormValues) {
    if (!selectedVersionId) return
    createScenarioMutation.mutate({
      params: { path: { versionId: selectedVersionId } },
      body: {
        name: values.name,
        type: values.type,
        utilityRate: values.utilityRate / 100,
        indirectRate: values.indirectRate / 100,
        contingencyRate: values.contingencyRate / 100,
        ...(values.durationDays ? { durationDays: values.durationDays } : {}),
        ...(values.constructionMethod ? { constructionMethod: values.constructionMethod } : {}),
        overrides: [],
      }
    })
  }

  return (
    <div className="space-y-6 animate-fade-in">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 border-b border-border pb-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Escenarios del Presupuesto</h1>
          <p className="text-sm text-muted-foreground">Evalúa alternativas de obra variando indirectos, rendimientos y contingencias.</p>
        </div>
        {selectedVersionId && (
          <Button size="sm" onClick={() => setShowModal(true)}>
            <Plus className="size-4" />
            Nuevo Escenario
          </Button>
        )}
      </div>

      {selectedVersionId ? (
        <div className="grid gap-6">
          {/* Scenarios comparison cards */}
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {scenariosQuery.data?.map(scenario => (
              <div
                key={scenario.id}
                className={`rounded-xl border p-5 bg-panel space-y-4 relative ${
                  scenario.primary ? 'border-primary shadow-xs ring-1 ring-primary/20' : 'border-border'
                }`}
              >
                {scenario.primary && (
                  <span className="absolute top-4 right-4 inline-flex items-center gap-1 bg-primary/10 text-primary text-[10px] font-bold px-2 py-0.5 rounded-full">
                    <Star className="size-3 fill-primary" />
                    PRINCIPAL
                  </span>
                )}

                <div>
                  <span className="text-[10px] bg-secondary px-2 py-0.5 rounded font-bold text-muted-foreground">
                    {scenario.type}
                  </span>
                  <h3 className="font-bold text-base mt-2">{scenario.name}</h3>
                </div>

                <div className="grid grid-cols-2 gap-2 text-xs">
                  <div>
                    <span className="text-muted-foreground block">Costo Directo</span>
                    <span className="font-bold font-mono">$ {scenario.totalCost?.toLocaleString(undefined, { minimumFractionDigits: 2 })}</span>
                  </div>
                  <div>
                    <span className="text-muted-foreground block">Precio de Venta</span>
                    <span className="font-bold font-mono text-primary">$ {scenario.salePrice?.toLocaleString(undefined, { minimumFractionDigits: 2 })}</span>
                  </div>
                </div>

                <div className="grid grid-cols-3 gap-1.5 text-[10px] border-t border-border pt-3 text-muted-foreground">
                  <div>
                    <span>Util: {((scenario.utilityRate || 0) * 100).toFixed(0)}%</span>
                  </div>
                  <div>
                    <span>Ind: {((scenario.indirectRate || 0) * 100).toFixed(0)}%</span>
                  </div>
                  <div>
                    <span>Cont: {((scenario.contingencyRate || 0) * 100).toFixed(0)}%</span>
                  </div>
                </div>

                {/* Actions */}
                {!scenario.primary && (
                  <div className="border-t border-border pt-3 flex justify-end">
                    <Button
                      size="sm"
                      variant="ghost"
                      onClick={() => makePrimaryMutation.mutate({ params: { path: { id: scenario.id! } } })}
                      disabled={makePrimaryMutation.isPending}
                    >
                      <CheckCircle className="size-3.5" />
                      Hacer Principal
                    </Button>
                  </div>
                )}
              </div>
            ))}
          </div>

          {/* Side by side comparison table */}
          {scenariosQuery.data && scenariosQuery.data.length > 1 && (
            <div className="rounded-xl border border-border bg-panel p-5 space-y-4">
              <h3 className="font-bold text-sm flex items-center gap-1.5">
                <Scale className="size-4 text-primary" />
                Matriz Comparativa de Escenarios
              </h3>

              <div className="overflow-x-auto">
                <table className="w-full text-left text-sm border-collapse">
                  <thead>
                    <tr className="border-b border-border text-muted-foreground text-xs font-semibold">
                      <th className="py-2">Escenario</th>
                      <th className="py-2">Tipo</th>
                      <th className="py-2 text-right">Costo Directo</th>
                      <th className="py-2 text-right">Indirectos / Utilidad / Contingencia</th>
                      <th className="py-2 text-right">Precio Venta</th>
                      <th className="py-2 text-right">Margen</th>
                      <th className="py-2">Riesgo</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-border">
                    {scenariosQuery.data?.map(scenario => (
                      <tr key={scenario.id} className={scenario.primary ? 'bg-primary/5 font-semibold' : ''}>
                        <td className="py-3 font-medium">{scenario.name}</td>
                        <td className="py-3 text-xs">{scenario.type}</td>
                        <td className="py-3 text-right font-mono text-xs">$ {scenario.totalCost?.toFixed(2)}</td>
                        <td className="py-3 text-right text-xs text-muted-foreground">
                          {((scenario.indirectRate || 0)*100).toFixed(0)}% / {((scenario.utilityRate || 0)*100).toFixed(0)}% / {((scenario.contingencyRate || 0)*100).toFixed(0)}%
                        </td>
                        <td className="py-3 text-right font-mono font-bold text-xs text-primary">$ {scenario.salePrice?.toFixed(2)}</td>
                        <td className="py-3 text-right font-mono text-xs">{((scenario.margin || 0) * 100).toFixed(1)}%</td>
                        <td className="py-3 text-xs">
                          <span className={`inline-flex rounded-full px-2 py-0.5 text-[10px] font-bold ${
                            scenario.risk === 'BAJO' ? 'bg-emerald-500/10 text-emerald-500' : 'bg-amber-500/10 text-amber-500'
                          }`}>
                            {scenario.risk || 'BAJO'}
                          </span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </div>
      ) : (
        <div className="rounded-xl border border-dashed border-border bg-panel/30 p-12 text-center text-muted-foreground h-64 flex flex-col items-center justify-center">
          <ShieldAlert className="size-8 text-muted-foreground/45 mb-2" />
          No se ha seleccionado ninguna versión de presupuesto. Por favor, selecciona una versión en la pestaña Presupuestos.
        </div>
      )}

      {/* Create Modal */}
      {showModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs">
          <div className="w-full max-w-md bg-panel border border-border p-6 rounded-xl shadow-lg space-y-4 animate-scale-up">
            <h2 className="text-lg font-bold border-b border-border pb-2">Crear Escenario Alternativo</h2>
            <form onSubmit={scenarioForm.handleSubmit((v) => handleCreateScenario(v))} className="space-y-4">
              <div>
                <label className="text-xs font-semibold text-muted-foreground mb-1 block">Nombre de Escenario</label>
                <Input placeholder="Ej. Escenario de Licitación Ajustado" {...scenarioForm.register('name')} />
                {scenarioForm.formState.errors.name && (
                  <p className="mt-1 text-xs text-destructive">{scenarioForm.formState.errors.name.message}</p>
                )}
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="text-xs font-semibold text-muted-foreground mb-1 block">Tipo de Escenario</label>
                  <select
                    className="flex h-9 w-full rounded-md border border-border bg-background px-3 py-1 text-sm shadow-xs focus-visible:outline-hidden"
                    {...scenarioForm.register('type')}
                  >
                    <option value="BASE">Base</option>
                    <option value="ECONOMICO">Económico</option>
                    <option value="RECOMENDADO">Recomendado</option>
                    <option value="CONSERVADOR">Conservador</option>
                    <option value="PERSONALIZADO">Personalizado</option>
                  </select>
                </div>
                <div>
                  <label className="text-xs font-semibold text-muted-foreground mb-1 block">Plazo de Obra (días)</label>
                  <Input type="number" {...scenarioForm.register('durationDays')} />
                </div>
              </div>

              <div className="grid grid-cols-3 gap-3">
                <div>
                  <label className="text-xs font-semibold text-muted-foreground mb-1 block">Utilidad %</label>
                  <Input type="number" step="0.01" {...scenarioForm.register('utilityRate')} />
                </div>
                <div>
                  <label className="text-xs font-semibold text-muted-foreground mb-1 block">Indirectos %</label>
                  <Input type="number" step="0.01" {...scenarioForm.register('indirectRate')} />
                </div>
                <div>
                  <label className="text-xs font-semibold text-muted-foreground mb-1 block">Contingencia %</label>
                  <Input type="number" step="0.01" {...scenarioForm.register('contingencyRate')} />
                </div>
              </div>

              <div className="flex justify-end gap-2 border-t border-border pt-3">
                <Button type="button" variant="outline" onClick={() => setShowModal(false)}>
                  Cancelar
                </Button>
                <Button type="submit" disabled={createScenarioMutation.isPending}>
                  Crear Escenario
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}
