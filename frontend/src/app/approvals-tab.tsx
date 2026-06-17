import { useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import * as zod from 'zod'
import { toast } from 'sonner'
import { CheckSquare, AlertCircle, AlertOctagon, CornerDownRight, MessageSquare, Check } from 'lucide-react'

import { api } from '@/lib/api/client'
import { apiErrorMessage } from '@/lib/api/errors'
import { Button } from '@/components/ui/button'

const decisionSchema = zod.object({
  comment: zod.string().min(3, 'Mínimo 3 caracteres para justificar la decisión'),
})
type DecisionFormValues = zod.infer<typeof decisionSchema>

interface ApprovalsTabProps {
  selectedVersionId: string | null
}

export function ApprovalsTab({ selectedVersionId }: ApprovalsTabProps) {
  const queryClient = useQueryClient()
  const [commentType, setCommentType] = useState<'observe' | 'reject' | null>(null)

  // Queries
  const approvalsQuery = useQuery({
    ...api.queryOptions('get', '/api/budget-versions/{versionId}/approvals', {
      params: { path: { versionId: selectedVersionId || '' } }
    }),
    enabled: !!selectedVersionId,
  })

  const activeRequest = approvalsQuery.data?.[approvalsQuery.data.length - 1]

  const commentsQuery = useQuery({
    ...api.queryOptions('get', '/api/approvals/{requestId}/comments', {
      params: { path: { requestId: activeRequest?.id || '' } }
    }),
    enabled: !!activeRequest?.id,
  })

  // Mutations
  const approveMutation = api.useMutation('put', '/api/approvals/{requestId}/approve', {
    onSuccess: () => {
      toast.success('Presupuesto APROBADO. La versión ha sido bloqueada y ahora es inmutable.')
      queryClient.invalidateQueries({
        queryKey: ['get', '/api/budget-versions/{versionId}/approvals', { params: { path: { versionId: selectedVersionId! } } }]
      })
      queryClient.invalidateQueries({
        queryKey: ['get', '/api/budget-versions/{id}', { params: { path: { id: selectedVersionId! } } }]
      })
    },
    onError: (err) => toast.error(apiErrorMessage(err, 'Error al aprobar presupuesto')),
  })

  const observeMutation = api.useMutation('put', '/api/approvals/{requestId}/observe', {
    onSuccess: () => {
      toast.success('Presupuesto observado y devuelto a ajustes')
      queryClient.invalidateQueries({
        queryKey: ['get', '/api/budget-versions/{versionId}/approvals', { params: { path: { versionId: selectedVersionId! } } }]
      })
      queryClient.invalidateQueries({
        queryKey: ['get', '/api/budget-versions/{id}', { params: { path: { id: selectedVersionId! } } }]
      })
      setCommentType(null)
      decisionForm.reset()
    },
    onError: (err) => toast.error(apiErrorMessage(err, 'Error al observar presupuesto')),
  })

  const rejectMutation = api.useMutation('put', '/api/approvals/{requestId}/reject', {
    onSuccess: () => {
      toast.success('Presupuesto RECHAZADO')
      queryClient.invalidateQueries({
        queryKey: ['get', '/api/budget-versions/{versionId}/approvals', { params: { path: { versionId: selectedVersionId! } } }]
      })
      queryClient.invalidateQueries({
        queryKey: ['get', '/api/budget-versions/{id}', { params: { path: { id: selectedVersionId! } } }]
      })
      setCommentType(null)
      decisionForm.reset()
    },
    onError: (err) => toast.error(apiErrorMessage(err, 'Error al rechazar presupuesto')),
  })

  // Forms
  const decisionForm = useForm<DecisionFormValues>({
    resolver: zodResolver(decisionSchema),
    defaultValues: { comment: '' }
  })

  function handleDecisionSubmit(values: DecisionFormValues) {
    if (!activeRequest?.id) return
    if (commentType === 'observe') {
      observeMutation.mutate({
        params: { path: { requestId: activeRequest.id } },
        body: { comment: values.comment }
      })
    } else if (commentType === 'reject') {
      rejectMutation.mutate({
        params: { path: { requestId: activeRequest.id } },
        body: { comment: values.comment }
      })
    }
  }

  return (
    <div className="space-y-6 animate-fade-in">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 border-b border-border pb-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Flujo de Aprobaciones</h1>
          <p className="text-sm text-muted-foreground">Aprueba, observa o rechaza revisiones de presupuestos de construcción.</p>
        </div>
      </div>

      {selectedVersionId ? (
        <div className="grid gap-6 md:grid-cols-[1fr_360px]">
          {/* Main decision card */}
          {activeRequest ? (
            <div className="rounded-xl border border-border bg-panel p-5 space-y-6">
              <div className="flex justify-between items-center border-b border-border pb-3">
                <h2 className="text-base font-bold flex items-center gap-1.5">
                  <CheckSquare className="size-5 text-primary" />
                  Solicitud Activa de Aprobación
                </h2>
                <span className={`inline-flex rounded-full px-2.5 py-0.5 text-xs font-bold ${
                  activeRequest.status === 'APROBADO'
                    ? 'bg-emerald-500/10 text-emerald-500'
                    : activeRequest.status === 'PENDIENTE_APROBACION'
                    ? 'bg-amber-500/10 text-amber-500'
                    : 'bg-destructive/10 text-destructive'
                }`}>
                  {activeRequest.status}
                </span>
              </div>

              {/* Status details */}
              <div className="space-y-4">
                <p className="text-sm text-muted-foreground">
                  {activeRequest.status === 'PENDIENTE_APROBACION' && (
                    'Esta revisión está esperando ser revisada por un Aprobador técnico. Al aprobarla, se congelará y será inmutable.'
                  )}
                  {activeRequest.status === 'APROBADO' && (
                    'Esta revisión ha sido aprobada con éxito. No se permiten más modificaciones sobre este árbol de presupuesto.'
                  )}
                  {activeRequest.status === 'OBSERVADO' && (
                    'Se han realizado observaciones técnicas. Requiere ajustes antes de reenviarse.'
                  )}
                  {activeRequest.status === 'RECHAZADO' && (
                    'Esta versión de presupuesto ha sido rechazada por viabilidad o costos comerciales.'
                  )}
                </p>

                {activeRequest.status === 'PENDIENTE_APROBACION' && !commentType && (
                  <div className="flex gap-3 border-t border-border pt-4">
                    <Button
                      className="bg-emerald-500 text-white hover:bg-emerald-600 flex-1"
                      onClick={() => approveMutation.mutate({ params: { path: { requestId: activeRequest.id || '' } } })}
                      disabled={approveMutation.isPending}
                    >
                      <Check className="size-4" />
                      Aprobar y Bloquear
                    </Button>
                    <Button
                      variant="outline"
                      className="text-amber-500 border-amber-500/20 hover:bg-amber-500/10 flex-1"
                      onClick={() => setCommentType('observe')}
                    >
                      <AlertCircle className="size-4" />
                      Observar
                    </Button>
                    <Button
                      variant="outline"
                      className="text-destructive border-destructive/20 hover:bg-destructive/10 flex-1"
                      onClick={() => setCommentType('reject')}
                    >
                      <AlertOctagon className="size-4" />
                      Rechazar
                    </Button>
                  </div>
                )}

                {commentType && (
                  <form onSubmit={decisionForm.handleSubmit(handleDecisionSubmit)} className="border border-border/80 bg-background p-4 rounded-xl space-y-4 animate-scale-up">
                    <h3 className="font-bold text-xs flex items-center gap-1.5">
                      {commentType === 'observe' ? (
                        <>
                          <AlertCircle className="size-4 text-amber-500" />
                          Añadir Observación de Ajustes
                        </>
                      ) : (
                        <>
                          <AlertOctagon className="size-4 text-destructive" />
                          Motivo del Rechazo de Presupuesto
                        </>
                      )}
                    </h3>

                    <div>
                      <textarea
                        className="flex min-h-20 w-full rounded-md border border-border bg-background px-3 py-2 text-sm shadow-xs focus-visible:outline-hidden"
                        placeholder="Escriba aquí los detalles y justificaciones..."
                        {...decisionForm.register('comment')}
                      />
                      {decisionForm.formState.errors.comment && (
                        <p className="mt-1 text-xs text-destructive">{decisionForm.formState.errors.comment.message}</p>
                      )}
                    </div>

                    <div className="flex justify-end gap-2 text-xs">
                      <Button type="button" variant="ghost" size="sm" onClick={() => setCommentType(null)}>
                        Cancelar
                      </Button>
                      <Button
                        type="submit"
                        size="sm"
                        className={commentType === 'observe' ? 'bg-amber-500 hover:bg-amber-600 text-white' : 'bg-destructive hover:bg-destructive/90 text-white'}
                        disabled={observeMutation.isPending || rejectMutation.isPending}
                      >
                        Enviar Decisión
                      </Button>
                    </div>
                  </form>
                )}
              </div>
            </div>
          ) : (
            <div className="rounded-xl border border-dashed border-border bg-panel/30 p-12 text-center text-muted-foreground flex flex-col items-center justify-center h-48">
              <CheckSquare className="size-8 text-muted-foreground/45 mb-2" />
              Esta versión aún no ha sido enviada para aprobación técnica.
            </div>
          )}

          {/* Comments sidebar */}
          <div className="rounded-xl border border-border bg-panel p-5 space-y-4">
            <h3 className="font-bold text-sm border-b border-border pb-2 flex items-center gap-1.5">
              <MessageSquare className="size-4 text-primary" />
              Historial de Observaciones
            </h3>

            {commentsQuery.isLoading ? (
              <div className="h-10 w-full animate-pulse bg-muted rounded-md" />
            ) : (
              <div className="space-y-3 max-h-96 overflow-y-auto">
                {commentsQuery.data?.map(c => (
                  <div key={c.id} className="border border-border/80 bg-background p-3 rounded-lg text-xs space-y-1.5">
                    <p className="font-semibold text-foreground flex items-center gap-1">
                      <CornerDownRight className="size-3 text-muted-foreground" />
                      Observación:
                    </p>
                    <p className="text-muted-foreground font-medium pl-4 italic">"{c.comment}"</p>
                    {c.response && (
                      <div className="border-t border-border/40 pt-1.5 mt-1.5 pl-4">
                        <p className="font-semibold text-primary">Respuesta de Ajuste:</p>
                        <p className="text-foreground mt-0.5">"{c.response}"</p>
                      </div>
                    )}
                  </div>
                ))}
                {(!commentsQuery.data || commentsQuery.data.length === 0) && (
                  <p className="text-center text-xs text-muted-foreground py-4">No se registran observaciones.</p>
                )}
              </div>
            )}
          </div>
        </div>
      ) : (
        <div className="rounded-xl border border-dashed border-border bg-panel/30 p-12 text-center text-muted-foreground h-64 flex flex-col items-center justify-center">
          <AlertCircle className="size-8 text-muted-foreground/45 mb-2" />
          No se ha seleccionado ninguna versión de presupuesto. Por favor, selecciona una versión en la pestaña Presupuestos.
        </div>
      )}
    </div>
  )
}
