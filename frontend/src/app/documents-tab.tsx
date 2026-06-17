import { useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { FileText, Download, ShieldAlert, CheckCircle, Sparkles } from 'lucide-react'

import { api } from '@/lib/api/client'
import { apiErrorMessage } from '@/lib/api/errors'
import { Button } from '@/components/ui/button'
import { useSessionStore } from '@/lib/auth/session-store'
import { env } from '@/lib/env'

interface DocumentsTabProps {
  selectedVersionId: string | null
}

export function DocumentsTab({ selectedVersionId }: DocumentsTabProps) {
  const queryClient = useQueryClient()
  const [generatingType, setGeneratingType] = useState<string | null>(null)

  // Queries
  const documentsQuery = useQuery({
    ...api.queryOptions('get', '/api/budget-versions/{versionId}/documents', { params: { path: { versionId: selectedVersionId || '' } } }),
    enabled: !!selectedVersionId,
  })

  async function handleGeneratePdf(type: string) {
    const token = useSessionStore.getState().accessToken
    const orgId = useSessionStore.getState().activeOrganizationId
    if (!selectedVersionId) return

    setGeneratingType(type)
    const url = `${env.apiBaseUrl}/api/budget-versions/${selectedVersionId}/documents?type=${type}`

    try {
      const res = await fetch(url, {
        method: 'POST',
        headers: {
          'Authorization': token ? `Bearer ${token}` : '',
          'X-Organization-Id': orgId || '',
        },
      })

      if (!res.ok) {
        throw new Error('Error al generar el PDF en el servidor')
      }

      const blob = await res.blob()
      const downloadUrl = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = downloadUrl
      link.download = `${type.toLowerCase()}-${selectedVersionId}.pdf`
      document.body.appendChild(link)
      link.click()
      link.remove()
      window.URL.revokeObjectURL(downloadUrl)

      toast.success('Documento PDF generado y descargado correctamente')
      queryClient.invalidateQueries({ queryKey: ['get', '/api/budget-versions/{versionId}/documents', { params: { versionId: selectedVersionId } }] })
    } catch (error) {
      toast.error(apiErrorMessage(error, 'No se pudo generar el documento PDF'))
    } finally {
      setGeneratingType(null)
    }
  }

  return (
    <div className="space-y-6 animate-fade-in">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 border-b border-border pb-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Reportes y Documentos PDF</h1>
          <p className="text-sm text-muted-foreground">Genera informes formales listos para el cliente comercial.</p>
        </div>
      </div>

      {selectedVersionId ? (
        <div className="grid gap-6 lg:grid-cols-[1fr_320px]">
          {/* Generation templates */}
          <div className="space-y-4">
            <div className="rounded-xl border border-border bg-panel p-4 flex items-start gap-3 text-xs text-muted-foreground">
              <ShieldAlert className="size-5 text-primary shrink-0 mt-0.5" />
              <p>
                <strong>Protección de información interna:</strong> Todos los documentos PDF generados desde esta superficie omiten automáticamente rendimientos de mano de obra, factores de desperdicios, costos directos internos y márgenes comerciales detallados, resguardando la competitividad de la constructora.
              </p>
            </div>

            <div className="grid gap-4 sm:grid-cols-1">
              {/* Cotización */}
              <div className="rounded-xl border border-border bg-panel p-5 flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
                <div className="space-y-1">
                  <h3 className="font-bold text-sm flex items-center gap-1.5">
                    <FileText className="size-4 text-primary" />
                    Cotización Simplificada
                  </h3>
                  <p className="text-xs text-muted-foreground">Formato de cotización formal simplificado mostrando capítulos principales y precio global ofertado.</p>
                </div>
                <Button
                  size="sm"
                  onClick={() => handleGeneratePdf('COTIZACION')}
                  disabled={generatingType !== null}
                >
                  <Download className="size-4" />
                  {generatingType === 'COTIZACION' ? 'Generando...' : 'Generar PDF'}
                </Button>
              </div>

              {/* Presupuesto Detallado */}
              <div className="rounded-xl border border-border bg-panel p-5 flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
                <div className="space-y-1">
                  <h3 className="font-bold text-sm flex items-center gap-1.5">
                    <FileText className="size-4 text-primary" />
                    Presupuesto Detallado de Rubros
                  </h3>
                  <p className="text-xs text-muted-foreground">Listado estructurado por capítulos y subcapítulos con desglose de cantidades de obra y precios unitarios ofertados.</p>
                </div>
                <Button
                  size="sm"
                  onClick={() => handleGeneratePdf('PRESUPUESTO_DETALLADO')}
                  disabled={generatingType !== null}
                >
                  <Download className="size-4" />
                  {generatingType === 'PRESUPUESTO_DETALLADO' ? 'Generando...' : 'Generar PDF'}
                </Button>
              </div>

              {/* Resumen por Capítulos */}
              <div className="rounded-xl border border-border bg-panel p-5 flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
                <div className="space-y-1">
                  <h3 className="font-bold text-sm flex items-center gap-1.5">
                    <FileText className="size-4 text-primary" />
                    Resumen Financiero por Capítulos
                  </h3>
                  <p className="text-xs text-muted-foreground">Resumen de costo consolidado agrupado únicamente por capítulos estructurales de la obra.</p>
                </div>
                <Button
                  size="sm"
                  onClick={() => handleGeneratePdf('RESUMEN_CAPITULOS')}
                  disabled={generatingType !== null}
                >
                  <Download className="size-4" />
                  {generatingType === 'RESUMEN_CAPITULOS' ? 'Generando...' : 'Generar PDF'}
                </Button>
              </div>
            </div>
          </div>

          {/* Generated history sidebar */}
          <div className="rounded-xl border border-border bg-panel p-5 space-y-4">
            <h3 className="font-bold text-sm border-b border-border pb-2 flex items-center gap-1.5">
              <CheckCircle className="size-4 text-primary" />
              Documentos Emitidos
            </h3>
            {documentsQuery.isLoading ? (
              <div className="h-10 w-full animate-pulse bg-muted rounded-md" />
            ) : (
              <div className="space-y-3 max-h-96 overflow-y-auto">
                {documentsQuery.data?.map(doc => (
                  <div key={doc.id} className="border border-border/80 bg-background p-3 rounded-lg text-xs space-y-1">
                    <p className="font-semibold text-foreground">{doc.type}</p>
                    <p className="text-[10px] text-muted-foreground font-mono">Número: {doc.number}</p>
                    <p className="text-[10px] text-muted-foreground">Formato: {doc.format}</p>
                  </div>
                ))}
                {(!documentsQuery.data || documentsQuery.data.length === 0) && (
                  <p className="text-center text-xs text-muted-foreground py-4">Aún no se registran descargas históricas.</p>
                )}
              </div>
            )}
          </div>
        </div>
      ) : (
        <div className="rounded-xl border border-dashed border-border bg-panel/30 p-12 text-center text-muted-foreground h-64 flex flex-col items-center justify-center">
          <Sparkles className="size-8 text-muted-foreground/45 mb-2" />
          No se ha seleccionado ninguna versión de presupuesto. Por favor, selecciona una versión en la pestaña Presupuestos.
        </div>
      )}
    </div>
  )
}
